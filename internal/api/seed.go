package api

import (
	"context"
	"fmt"
	"strings"
	"time"

	"golang.org/x/crypto/bcrypt"
)

// seed creates an idempotent, realistic local-only demonstration dataset.
func (a *App) seed(ctx context.Context) error {
	var exists bool
	if err := a.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM sisges.users WHERE register='ADM0001' AND deleted_at IS NULL)`).Scan(&exists); err != nil || exists {
		return err
	}
	tx, err := a.db.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	hash, err := bcrypt.GenerateFromPassword([]byte("admin123"), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	var adminID int
	if err := tx.QueryRow(ctx, `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role) VALUES($1,$2,$3,$4,$5,$6,'ADMIN') RETURNING id`, "Administrador SISGES", "adm0001@sisges.com", "ADM0001", string(hash), time.Date(1980, 1, 1, 0, 0, 0, 0, time.UTC), "OTHER").Scan(&adminID); err != nil {
		return err
	}
	localEmail := func(name string) string {
		replacer := strings.NewReplacer("á", "a", "à", "a", "ã", "a", "â", "a", "é", "e", "ê", "e", "í", "i", "ó", "o", "ô", "o", "õ", "o", "ú", "u", "ç", "c")
		return replacer.Replace(strings.ToLower(strings.ReplaceAll(name, " ", "."))) + "@sisges.local"
	}
	classIDs := make([]int, 0, 10)
	for i := 1; i <= 10; i++ {
		var id int
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.school_class(name,academic_year) VALUES($1,$2) RETURNING id`, fmt.Sprintf("Turma %02d", i), "6º ano").Scan(&id); err != nil {
			return fmt.Errorf("seed class %d: %w", i, err)
		}
		classIDs = append(classIDs, id)
	}
	disciplineIDs := make([]int, 0, 5)
	for _, name := range []string{"Português", "Matemática", "Ciências", "História", "Geografia", "Inglês", "Artes", "Educação Física", "Filosofia", "Sociologia", "Tecnologia", "Projeto de Vida"} {
		var id int
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.discipline(name,description) VALUES($1,'Componente curricular') RETURNING id`, name).Scan(&id); err != nil {
			return fmt.Errorf("seed discipline %s: %w", name, err)
		}
		disciplineIDs = append(disciplineIDs, id)
	}
	for _, classID := range classIDs {
		for _, disciplineID := range disciplineIDs {
			if _, err := tx.Exec(ctx, `INSERT INTO sisges.class_discipline(class_id,discipline_id) VALUES($1,$2)`, classID, disciplineID); err != nil {
				return fmt.Errorf("seed class disciplines: %w", err)
			}
		}
	}
	teacherNames := []string{"Ana Clara Ferreira", "Bruno Henrique Costa", "Camila Rodrigues", "Daniel Martins", "Elisa Moreira", "Felipe Nascimento", "Gabriela Souza", "Henrique Lima", "Isabela Carvalho", "João Pedro Alves", "Larissa Teixeira", "Marcelo Barros", "Natália Ribeiro", "Otávio Freitas", "Priscila Azevedo"}
	for index, name := range teacherNames {
		i := index + 1
		var userID, teacherID int
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role) VALUES($1,$2,$3,$4,$5,$6,'TEACHER') RETURNING id`, name, localEmail(name), fmt.Sprintf("PRO%04d", i), string(hash), time.Date(1980+i%15, time.Month(i%12+1), i%28+1, 0, 0, 0, 0, time.UTC), "OTHER").Scan(&userID); err != nil {
			return fmt.Errorf("seed teacher user %d: %w", i, err)
		}
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.teacher(user_id) VALUES($1) RETURNING id`, userID).Scan(&teacherID); err != nil {
			return fmt.Errorf("seed teacher %d: %w", i, err)
		}
		for offset := 0; offset < 2; offset++ {
			if _, err := tx.Exec(ctx, `INSERT INTO sisges.teacher_class(teacher_id,class_id) VALUES($1,$2)`, teacherID, classIDs[(i+offset-1)%len(classIDs)]); err != nil {
				return fmt.Errorf("seed teacher class: %w", err)
			}
		}
	}
	studentFirstNames := []string{"Alice", "Arthur", "Beatriz", "Bernardo", "Cecília", "Caio", "Davi", "Elena", "Enzo", "Fernanda", "Gabriel", "Helena", "Igor", "Júlia", "Lucas", "Manuela", "Miguel", "Nicolas", "Olívia", "Pedro", "Rafael", "Sofia", "Theo", "Valentina", "Vinícius"}
	studentLastNames := []string{"Almeida", "Barbosa", "Cardoso", "Dias", "Esteves", "Farias", "Gomes", "Mendes", "Pereira", "Rocha"}
	responsibleFirstNames := []string{"Adriana", "Alexandre", "Bianca", "Carlos", "Daniela", "Eduardo", "Fabiana", "Gustavo", "Juliana", "Leandro", "Mariana", "Paulo", "Renata", "Ricardo", "Tatiana", "Victor", "Yasmin", "André", "Cláudia", "Diego", "Fernanda", "Heloísa", "Leonardo", "Patrícia", "Roberto"}
	type responsiblePair struct{ first, second int }
	siblingResponsibles := map[int]responsiblePair{}
	createResponsible := func(name, suffix string) (int, error) {
		var id int
		email := strings.Replace(localEmail(name), "@sisges.local", suffix+"@sisges.local", 1)
		err := tx.QueryRow(ctx, `INSERT INTO sisges.student_responsible(name,phone,alternative_phone,email,alternative_email) VALUES($1,$2,$3,$4,$5) RETURNING id`, name, "(11) 90000-0000", "(11) 98888-0000", email, "contato"+suffix+"@sisges.local").Scan(&id)
		return id, err
	}
	for i := 1; i <= 250; i++ {
		studentName := fmt.Sprintf("%s %s", studentFirstNames[(i-1)%len(studentFirstNames)], studentLastNames[(i-1)/len(studentFirstNames)])
		responsibleName := fmt.Sprintf("%s %s", responsibleFirstNames[(i-1)%len(responsibleFirstNames)], studentLastNames[(i-1)/len(studentFirstNames)])
		var userID, studentID int
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role) VALUES($1,$2,$3,$4,$5,$6,'STUDENT') RETURNING id`, studentName, localEmail(studentName), fmt.Sprintf("ALU%04d", i), string(hash), time.Date(2013+i%4, time.Month(i%12+1), i%28+1, 0, 0, 0, 0, time.UTC), "OTHER").Scan(&userID); err != nil {
			return fmt.Errorf("seed student user %d: %w", i, err)
		}
		if err := tx.QueryRow(ctx, `INSERT INTO sisges.student(user_id,class_id) VALUES($1,$2) RETURNING id`, userID, classIDs[(i-1)%len(classIDs)]).Scan(&studentID); err != nil {
			return fmt.Errorf("seed student %d: %w", i, err)
		}
		parentCount := 1
		if i <= 50 { // 20% of 250 students have two registered responsibles.
			parentCount = 2
		}
		group := -1
		if i <= 13 { // 13 students (5.2%) form five pairs and one sibling trio.
			group = (i - 1) / 2
			if i == 13 {
				group = 5
			}
		}
		pair, shared := siblingResponsibles[group]
		if !shared {
			first, err := createResponsible(responsibleName, fmt.Sprintf("%03d", i))
			if err != nil { return fmt.Errorf("seed responsible: %w", err) }
			pair.first = first
			if parentCount == 2 {
				secondName := fmt.Sprintf("%s %s", responsibleFirstNames[i%len(responsibleFirstNames)], studentLastNames[(i-1)/len(studentFirstNames)])
				second, err := createResponsible(secondName, fmt.Sprintf("a%03d", i))
				if err != nil { return fmt.Errorf("seed second responsible: %w", err) }
				pair.second = second
			}
			if group >= 0 { siblingResponsibles[group] = pair }
		}
		for _, responsibleID := range []int{pair.first, pair.second} {
			if responsibleID == 0 { continue }
			if _, err := tx.Exec(ctx, `INSERT INTO sisges.student_responsible_link(student_id,responsible_id) VALUES($1,$2)`, studentID, responsibleID); err != nil {
				return fmt.Errorf("seed student responsible: %w", err)
			}
		}
	}
	for i := 1; i <= 20; i++ {
		if _, err := tx.Exec(ctx, `INSERT INTO sisges.announcement(title,content,type,target_roles,active_from,active_until,created_at,created_by) VALUES($1,$2,'TEXT','ADMIN,TEACHER,STUDENT',$3,$4,$3,$5)`, fmt.Sprintf("Comunicado %02d", i), fmt.Sprintf("Este é o comunicado de demonstração número %d para a comunidade escolar.", i), time.Now().Add(-time.Duration(21-i)*24*time.Hour), time.Now().AddDate(0, 3, 0), adminID); err != nil {
			return fmt.Errorf("seed announcement %d: %w", i, err)
		}
	}
	for i := 1; i <= 8; i++ {
		if _, err := tx.Exec(ctx, `INSERT INTO sisges.school_event(title,description,event_at,audience,created_by) VALUES($1,$2,$3,'ALL',$4)`, fmt.Sprintf("Evento encerrado %02d", i), "Evento de demonstração já realizado.", time.Now().Add(-time.Duration(i)*24*time.Hour), adminID); err != nil {
			return fmt.Errorf("seed past event %d: %w", i, err)
		}
	}
	for i := 1; i <= 25; i++ {
		audience := "ALL"
		var classID any
		if i%5 == 0 {
			audience = "CLASS"
			classID = classIDs[(i-1)%len(classIDs)]
		}
		if _, err := tx.Exec(ctx, `INSERT INTO sisges.school_event(title,description,event_at,audience,class_id,created_by) VALUES($1,$2,$3,$4,$5,$6)`, fmt.Sprintf("Evento escolar %02d", i), "Evento de demonstração agendado para a comunidade escolar.", time.Now().Add(time.Duration(i)*24*time.Hour).Add(9*time.Hour), audience, classID, adminID); err != nil {
			return fmt.Errorf("seed upcoming event %d: %w", i, err)
		}
	}
	return tx.Commit(ctx)
}

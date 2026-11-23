package api

import "testing"

func TestLegacyLessonTeacherOwnership(t *testing.T) {
	owner, colleague := 17, 23
	for _, tc := range []struct {
		name             string
		teacherID        int
		owner, requested *int
		allowed          bool
	}{
		{"mantém o professor atual", owner, &owner, nil, true},
		{"confirma o professor atual", owner, &owner, &owner, true},
		{"aula de outro professor", colleague, &owner, nil, false},
		{"mudança de professor", owner, &owner, &colleague, false},
		{"aula sem professor", owner, nil, nil, false},
		{"usuário sem perfil de professor", 0, &owner, nil, false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			req := meetingRequest{TeacherID: tc.requested}
			if got := req.canEditAsTeacher(tc.teacherID, tc.owner); got != tc.allowed {
				t.Fatalf("Permissão para editar a aula: recebeu %v, esperava %v", got, tc.allowed)
			}
		})
	}
}

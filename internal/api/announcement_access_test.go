package api

import (
	"strconv"
	"strings"
	"testing"
)

func TestAnnouncementAudiencePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	teacher := app.loginSeed("ana.clara.ferreira@sisges.local")
	student := app.loginSeed("alice.almeida@sisges.local")
	admin := app.loginSeed("adm0001@sisges.com")
	app.call("GET", "/announcements/feed", "", nil, 401, nil)

	var item announcementResponse
	app.call("POST", "/announcements", teacher, map[string]any{
		"title": "Aviso interno", "type": "TEXT", "hiddenForRoles": []string{"STUDENT"},
	}, 201, &item)
	path := "/announcements/" + strconv.Itoa(item.ID)
	t.Cleanup(func() { app.call("DELETE", path, admin, nil, 204, nil) })
	app.call("POST", path+"/comments", teacher, map[string]string{"content": "Confirmado."}, 201, nil)
	app.call("GET", path+"/comments", student, nil, 404, nil)
	app.call("GET", path+"/likes", student, nil, 404, nil)
	app.call("POST", path+"/like", student, nil, 404, nil)

	var feed []announcementResponse
	app.call("GET", "/announcements/feed", student, nil, 200, &feed)
	for _, announcement := range feed {
		if announcement.ID == item.ID {
			t.Fatal("o mural retornou um aviso oculto para o perfil do aluno")
		}
	}
}

func TestFeedSubscriptionFrame(t *testing.T) {
	command, headers := parseFeedFrame("\nSUBSCRIBE\r\nid:sub-0\r\ndestination:/topic/feed\r\n\r\n\x00")
	if command != "SUBSCRIBE" || headers["destination"] != "/topic/feed" {
		t.Fatal("o servidor não reconheceu a assinatura STOMP do mural")
	}
	hub := newFeedHub(2)
	client := &feedClient{send: make(chan []byte, 1)}
	hub.add(client)
	id := headers["id"]
	client.subscription.Store(&id)
	hub.broadcast("ANNOUNCEMENT_CREATED", 42)
	select {
	case frame := <-client.send:
		if !strings.Contains(string(frame), "\nsubscription:sub-0\n") || !strings.Contains(string(frame), `"announcementId":42`) {
			t.Fatalf("mensagem STOMP sem a assinatura ou o aviso esperado: %q", frame)
		}
	default:
		t.Fatal("a assinatura não recebeu a mensagem do mural")
	}
}

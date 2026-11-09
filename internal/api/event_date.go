package api

import (
	"time"
	_ "time/tzdata"
)

var eventLocation = func() *time.Location {
	location, err := time.LoadLocation("America/Sao_Paulo")
	if err != nil {
		panic(err)
	}
	return location
}()

func validEventDate(eventAt, now time.Time) bool {
	year := now.In(eventLocation).Year()
	end := time.Date(year+1, time.January, 1, 0, 0, 0, 0, eventLocation).Add(-time.Microsecond)
	return !eventAt.Before(now) && !eventAt.After(end)
}

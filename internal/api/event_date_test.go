package api

import (
	"testing"
	"time"
)

func TestEventDateLimits(t *testing.T) {
	now := time.Date(2026, time.November, 9, 19, 0, 0, 0, eventLocation)
	end := time.Date(2026, time.December, 31, 23, 59, 59, 999999000, eventLocation)
	for _, tc := range []struct {
		name string
		date time.Time
		valid bool
	}{
		{"exact now", now, true},
		{"past", now.Add(-time.Microsecond), false},
		{"future", now.Add(time.Hour), true},
		{"inclusive end", end, true},
		{"beyond end", end.Add(time.Microsecond), false},
		{"next year", end.Add(time.Hour), false},
	} {
		t.Run(tc.name, func(t *testing.T) {
			if got := validEventDate(tc.date, now); got != tc.valid {
				t.Fatalf("validEventDate=%v, want %v", got, tc.valid)
			}
		})
	}
	// UTC has advanced to January while the school is still in December.
	utcNow := time.Date(2027, 1, 1, 1, 0, 0, 0, time.UTC)
	if !validEventDate(end, utcNow) || validEventDate(end.Add(time.Hour), utcNow) {
		t.Fatal("year boundary must follow São Paulo, not server UTC")
	}
}

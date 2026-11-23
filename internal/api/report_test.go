package api

import "testing"

func TestAcademicYearProgression(t *testing.T) {
	for _, tc := range []struct {
		from, next string
		known      bool
	}{
		{"5º ano - Fundamental", "6º ano", true},
		{"8º ano", "9º ano", true},
		{"9º ano", "1º ano - Médio", true},
		{"3º ano - Médio", "", true},
		{"", "", false},
		{"4º ano - Médio", "", false},
	} {
		if next, known := nextAcademicYear(tc.from); next != tc.next || known != tc.known {
			t.Errorf("nextAcademicYear(%q) = (%q, %v), want (%q, %v)", tc.from, next, known, tc.next, tc.known)
		}
	}
}

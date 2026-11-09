package api

import (
	"reflect"
	"testing"
)

func TestTeacherLinkDisciplines(t *testing.T) {
	for _, tc := range []struct {
		name string
		available, selected, want []int
		invalid bool
	}{
		{"single automatic", []int{3}, nil, []int{3}, false},
		{"multiple require selection", []int{3, 4}, nil, nil, true},
		{"selected subset", []int{3, 4}, []int{4}, []int{4}, false},
		{"selected several", []int{3, 4}, []int{3, 4}, []int{3, 4}, false},
		{"duplicates", []int{3, 4}, []int{4, 4}, []int{4}, false},
		{"unrelated discipline", []int{3}, []int{9}, nil, true},
		{"no disciplines", nil, nil, nil, true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			got, message := teacherLinkDisciplines(tc.available, tc.selected)
			if (message != "") != tc.invalid || !reflect.DeepEqual(got, tc.want) {
				t.Fatalf("got %v, error %q; want %v, invalid %v", got, message, tc.want, tc.invalid)
			}
		})
	}
}

package org.lnu.timetable.generation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class EvaluatedTimetable {
    private final Lesson[] lessons;
    private final double penalty;
    private final double[] lessonPenalties;
    private final Map<Integer, Set<Integer>> lecturerLessonsMap;
    private final Map<Integer, Set<Integer>> academicGroupLessonsMap;
    private final Map<Integer, Set<Integer>> placeLessonsMap;

    public EvaluatedTimetable(Lesson[] lessons, double penalty, double[] lessonPenalties) {
        this.lessons = lessons;
        this.penalty = penalty;
        this.lessonPenalties = lessonPenalties;

        lecturerLessonsMap = null;
        academicGroupLessonsMap = null;
        placeLessonsMap = null;
    }
}

package org.lnu.timetable.generation.service;

import org.lnu.timetable.generation.model.Day;
import org.lnu.timetable.generation.model.EvaluatedTimetable;
import org.lnu.timetable.generation.model.Lesson;
import org.lnu.timetable.generation.model.LessonPeriodicity;
import org.lnu.timetable.generation.model.LessonPlace;
import org.lnu.timetable.generation.model.LessonRequirements;
import org.lnu.timetable.generation.model.LessonTimeSlot;
import org.lnu.timetable.generation.model.TimetableRequirements;
import org.lnu.timetable.generation.util.LessonTimeSlotUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Service
public class TimetableGenerationService {
    private static final Random random = new Random();
    private static final Day[] days = Day.values();

    private static final Comparator<Lesson> LESSON_COMPARATOR = comparing(Lesson::getDay)
            .thenComparing(Lesson::getTimeSlot)
            .thenComparing(Lesson::getPeriodicity);

    private static final Comparator<EvaluatedTimetable> TIMETABLE_COMPARATOR = comparing(EvaluatedTimetable::getPenalty);

    private record CrossoverResult(EvaluatedTimetable child1, EvaluatedTimetable child2) {
    }

    private final LessonTimeSlotUtil lessonTimeSlotUtil;

    private final int iterationsMaxCount;
    private final int populationInitialSize;

    private final double crossoverRate;
    private final double crossoverMinBadGeneRate;
    private final double crossoverMaxBadGeneRate;

    private final double mutationBadGenesRate;
    private final double mutationDayRate;
    private final double mutationTimeSlotRate;
    private final double mutationPlaceRate;
    private final double mutationPeriodicityRate;

    private final double lecturerConflictTimeSlotPenalty;
    private final double lecturerConflictTimeSlotPenaltyPower;
    private final double lecturerTimeWindowPenalty;
    private final double lecturerTimeWindowPenaltyPower;
    private final double lecturerTimeWindowPenaltyDayPower;

    private final double academicGroupConflictTimeSlotPenalty;
    private final double academicGroupConflictTimeSlotPenaltyPower;
    private final double academicGroupTimeWindowPenalty;
    private final double academicGroupTimeWindowPenaltyPower;
    private final double academicGroupTimeWindowPenaltyDayPower;

    private final double placeConflictTimeSlotPenalty;
    private final double placeConflictTimeSlotPenaltyPower;

    public TimetableGenerationService(
            LessonTimeSlotUtil lessonTimeSlotUtil,

            @Value("${iterations.max-count}") int iterationsMaxCount,
            @Value("${population.initial-size}") int populationInitialSize,

            @Value("${crossover.rate}") double crossoverRate,
            @Value("${crossover.rate.min_bad_gene}") double crossoverMinBadGeneRate,
            @Value("${crossover.rate.max_bad_gene}") double crossoverMaxBadGeneRate,

            @Value("${mutation.rate.bad_genes}") double mutationBadGenesRate,
            @Value("${mutation.rate.day}") double mutationDayRate,
            @Value("${mutation.rate.time_slot}") double mutationTimeSlotRate,
            @Value("${mutation.rate.place}") double mutationPlaceRate,
            @Value("${mutation.rate.periodicity}") double mutationPeriodicityRate,

            @Value("${penalty.lecturer.conflict_time_slot}") double lecturerConflictTimeSlotPenalty,
            @Value("${penalty.lecturer.conflict_time_slot.power}") double lecturerConflictTimeSlotPenaltyPower,
            @Value("${penalty.lecturer.time_window}") double lecturerTimeWindowPenalty,
            @Value("${penalty.lecturer.time_window.power}") double lecturerTimeWindowPenaltyPower,
            @Value("${penalty.lecturer.time_window.power.day}") double lecturerTimeWindowPenaltyDayPower,

            @Value("${penalty.academic_group.conflict_time_slot}") double academicGroupConflictTimeSlotPenalty,
            @Value("${penalty.academic_group.conflict_time_slot.power}") double academicGroupConflictTimeSlotPenaltyPower,
            @Value("${penalty.academic_group.time_window}") double academicGroupTimeWindowPenalty,
            @Value("${penalty.academic_group.time_window.power}") double academicGroupTimeWindowPenaltyPower,
            @Value("${penalty.academic_group.time_window.power.day}") double academicGroupTimeWindowPenaltyDayPower,

            @Value("${penalty.place.conflict_time_slot}") double placeConflictTimeSlotPenalty,
            @Value("${penalty.place.conflict_time_slot.power}") double placeConflictTimeSlotPenaltyPower
    ) {
        this.lessonTimeSlotUtil = lessonTimeSlotUtil;

        this.iterationsMaxCount = iterationsMaxCount;
        this.populationInitialSize = populationInitialSize;

        this.crossoverRate = crossoverRate;
        this.crossoverMinBadGeneRate = crossoverMinBadGeneRate;
        this.crossoverMaxBadGeneRate = crossoverMaxBadGeneRate;

        this.mutationBadGenesRate = mutationBadGenesRate;
        this.mutationDayRate = mutationDayRate;
        this.mutationTimeSlotRate = mutationTimeSlotRate;
        this.mutationPlaceRate = mutationPlaceRate;
        this.mutationPeriodicityRate = mutationPeriodicityRate;

        this.lecturerConflictTimeSlotPenalty = lecturerConflictTimeSlotPenalty;
        this.lecturerConflictTimeSlotPenaltyPower = lecturerConflictTimeSlotPenaltyPower;
        this.lecturerTimeWindowPenalty = lecturerTimeWindowPenalty;
        this.lecturerTimeWindowPenaltyPower = lecturerTimeWindowPenaltyPower;
        this.lecturerTimeWindowPenaltyDayPower = lecturerTimeWindowPenaltyDayPower;

        this.academicGroupConflictTimeSlotPenalty = academicGroupConflictTimeSlotPenalty;
        this.academicGroupConflictTimeSlotPenaltyPower = academicGroupConflictTimeSlotPenaltyPower;
        this.academicGroupTimeWindowPenalty = academicGroupTimeWindowPenalty;
        this.academicGroupTimeWindowPenaltyPower = academicGroupTimeWindowPenaltyPower;
        this.academicGroupTimeWindowPenaltyDayPower = academicGroupTimeWindowPenaltyDayPower;

        this.placeConflictTimeSlotPenalty = placeConflictTimeSlotPenalty;
        this.placeConflictTimeSlotPenaltyPower = placeConflictTimeSlotPenaltyPower;
    }

    public EvaluatedTimetable generateTimetable(TimetableRequirements timetableRequirements) {
        // Generate the initial population
        List<EvaluatedTimetable> population = generateInitialPopulation(timetableRequirements);

        EvaluatedTimetable bestTimetable = null;
        double penalty = Double.MAX_VALUE;
        int i = 0;
        for (; penalty > 0 && i < iterationsMaxCount; i++) {
            crossover(timetableRequirements, population);
            mutation(timetableRequirements, population);
//            population = repair(timetableRequirements, population);
            population = selection(population);

            bestTimetable = population.get(0);
            penalty = bestTimetable.getPenalty();
//            System.out.println(i + " - penalty: " + penalty);
            System.out.println(penalty);
        }
        System.out.println("i = " + i);

        return bestTimetable;
    }

    private List<EvaluatedTimetable> generateInitialPopulation(TimetableRequirements timetableRequirements) {
        List<EvaluatedTimetable> population = new ArrayList<>(populationInitialSize);

        for (int i = 0; i < populationInitialSize; i++) {
            Lesson[] lessons = generateInitialTimetable(timetableRequirements);
            EvaluatedTimetable timetable = evaluateTimetable(timetableRequirements, lessons);
            population.add(timetable);
        }

        return population;
    }

    private Lesson[] generateInitialTimetable(TimetableRequirements timetableRequirements) {
        List<LessonRequirements> lessonRequirementsList = timetableRequirements.getLessonRequirementsList();
        List<LessonTimeSlot> timeSlots = timetableRequirements.getTimeSlots();
        List<LessonPlace> lessonPlaces = timetableRequirements.getLessonPlaces();

        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < lessonRequirementsList.size(); ++i) {
            LessonRequirements lessonRequirements = lessonRequirementsList.get(i);
            double numberOfClassesPerWeek = lessonRequirements.getLessonsCountPerWeek();

            while (numberOfClassesPerWeek > 0) {
                Day day = getRandomDay();
                LessonTimeSlot timeSlot = getRandomTimeSlot(timeSlots);
                int lessonPlaceIndex = getRandomPlace(lessonPlaces);

                LessonPeriodicity lessonPeriodicity = getRandomLessonPeriodicity(numberOfClassesPerWeek);

                Lesson lesson = Lesson.builder()
                        .requirementsIndex(i)
                        .day(day)
                        .timeSlot(timeSlot)
                        .placeIndex(lessonPlaceIndex)
                        .periodicity(lessonPeriodicity)
                        .build();

                lessons.add(lesson);

                --numberOfClassesPerWeek;
            }
        }

        return lessons.toArray(Lesson[]::new);
    }

    private EvaluatedTimetable evaluateTimetable(TimetableRequirements timetableRequirements, Lesson[] lessons) {
        List<LessonRequirements> lessonRequirementsList = timetableRequirements.getLessonRequirementsList();

        Map<Integer, Set<Integer>> lecturerLessonsMap = new HashMap<>();
        Map<Integer, Set<Integer>> academicGroupLessonsMap = new HashMap<>();
        Map<Integer, Set<Integer>> placeLessonsMap = new HashMap<>();

        Comparator<Integer> lessonIndexcomparator = (index1, index2) -> {
            int comparisonResult = LESSON_COMPARATOR.compare(lessons[index1], lessons[index1]);
            return comparisonResult == 0 ? index1 - index2 : comparisonResult;
        };

        for (int i = 0; i < lessons.length; ++i) {
            Lesson lesson = lessons[i];

            LessonRequirements lessonRequirements = lessonRequirementsList.get(lesson.getRequirementsIndex());
            int lecturerIndex = lessonRequirements.getLecturerIndex();
            int placeIndex = lesson.getPlaceIndex();
            Set<Integer> academicGroupIndexes = lessonRequirements.getAcademicGroupIndexes();


            Set<Integer> lecturerLessonIndexes = lecturerLessonsMap.get(lecturerIndex);
            if (lecturerLessonIndexes == null) {
                lecturerLessonIndexes = new TreeSet<>(lessonIndexcomparator);
                lecturerLessonsMap.put(lecturerIndex, lecturerLessonIndexes);
            }
            lecturerLessonIndexes.add(i);

            for (int academicGroupIndex : academicGroupIndexes) {
                Set<Integer> academicGroupLessonIndexes = academicGroupLessonsMap.get(academicGroupIndex);
                if (academicGroupLessonIndexes == null) {
                    academicGroupLessonIndexes = new TreeSet<>(lessonIndexcomparator);
                    academicGroupLessonsMap.put(academicGroupIndex, academicGroupLessonIndexes);
                }
                academicGroupLessonIndexes.add(i);
            }


            Set<Integer> placeLessonIndexes = placeLessonsMap.get(placeIndex);
            if (placeLessonIndexes == null) {
                placeLessonIndexes = new TreeSet<>(lessonIndexcomparator);
                placeLessonsMap.put(placeIndex, placeLessonIndexes);
            }
            placeLessonIndexes.add(i);
        }

        double penalty = 0;
        double[] lessonPenalties = new double[lessons.length];

        for (Entry<Integer, Set<Integer>> lecturerLessonsEntry : lecturerLessonsMap.entrySet()) {
            int lecturerIndex = lecturerLessonsEntry.getKey();
            Set<Integer> lessonIndexes = lecturerLessonsEntry.getValue();

            double conflictTimeSlotsCount = 0;
            double timeWindowCount = 0;

            Iterator<Integer> lessonIndexIterator = lessonIndexes.iterator();
            Lesson prevLesson = null;
            int firstLessonIndex = 0;
            List<Integer> conflictTimeSlotLessonIndexes = new ArrayList<>();
            List<Integer> timeWindowLessonIndexes = new ArrayList<>();
            while (lessonIndexIterator.hasNext()) {
                double timeWindowDayCount = 0;

                List<Integer> dayLessonIndexes = new ArrayList<>();
                if (firstLessonIndex != 0) {
                    dayLessonIndexes.add(firstLessonIndex);
                }

                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        if (lesson.getDay() == prevLesson.getDay()) {
                            dayLessonIndexes.add(lessonIndex);

                            LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                    conflictTimeSlotLessonIndexes.add(lessonIndex);
                                }
                            }

                            if (lessonTimeSlotUtil.isWindow(prevLessonTimeSlot, lessonTimeSlot)) {
                                ++timeWindowDayCount;
                            }
                        } else {
                            isNewDay = true;
                            firstLessonIndex = lessonIndex;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }

                if (timeWindowDayCount > 0) {
                    timeWindowDayCount = Math.pow(timeWindowDayCount, lecturerTimeWindowPenaltyDayPower);

                    timeWindowCount += timeWindowDayCount;

                    timeWindowLessonIndexes.addAll(dayLessonIndexes);
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, lecturerConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * lecturerConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;

                double conflictTimeSlotPenaltyPerLesson = conflictTimeSlotPenalty / conflictTimeSlotLessonIndexes.size();
                for (int lessonIndex : conflictTimeSlotLessonIndexes) {
                    lessonPenalties[lessonIndex] += conflictTimeSlotPenaltyPerLesson;
                }
            }

            if (timeWindowCount > 0) {
                timeWindowCount = Math.pow(timeWindowCount, lecturerTimeWindowPenaltyPower);

                double timeWindowPenalty = timeWindowCount * lecturerTimeWindowPenalty;
                penalty += timeWindowPenalty;

                double timeWindowPenaltyPerLesson = timeWindowPenalty / timeWindowLessonIndexes.size();
                for (int lessonIndex : timeWindowLessonIndexes) {
                    lessonPenalties[lessonIndex] += timeWindowPenaltyPerLesson;
                }
            }
        }

        for (Entry<Integer, Set<Integer>> academicGroupLessonsEntry : academicGroupLessonsMap.entrySet()) {
            int academicGroupIndex = academicGroupLessonsEntry.getKey();
            Set<Integer> lessonIndexes = academicGroupLessonsEntry.getValue();

            double conflictTimeSlotsCount = 0;
            double timeWindowCount = 0;

            Iterator<Integer> lessonIndexIterator = lessonIndexes.iterator();
            Lesson prevLesson = null;
            int firstLessonIndex = 0;
            List<Integer> conflictTimeSlotLessonIndexes = new ArrayList<>();
            List<Integer> timeWindowLessonIndexes = new ArrayList<>();
            while (lessonIndexIterator.hasNext()) {
                double timeWindowDayCount = 0;

                List<Integer> dayLessonIndexes = new ArrayList<>();
                if (firstLessonIndex != 0) {
                    dayLessonIndexes.add(firstLessonIndex);
                }

                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        if (lesson.getDay() == prevLesson.getDay()) {
                            dayLessonIndexes.add(lessonIndex);

                            LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                    conflictTimeSlotLessonIndexes.add(lessonIndex);
                                }
                            }

                            if (lessonTimeSlotUtil.isWindow(prevLessonTimeSlot, lessonTimeSlot)) {
                                ++timeWindowDayCount;
                            }
                        } else {
                            isNewDay = true;
                            firstLessonIndex = lessonIndex;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }

                if (timeWindowDayCount > 0) {
                    timeWindowDayCount = Math.pow(timeWindowDayCount, academicGroupTimeWindowPenaltyDayPower);

                    timeWindowCount += timeWindowDayCount;

                    timeWindowLessonIndexes.addAll(dayLessonIndexes);
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, academicGroupConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * academicGroupConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;

                double conflictTimeSlotPenaltyPerLesson = conflictTimeSlotPenalty / conflictTimeSlotLessonIndexes.size();
                for (int lessonIndex : conflictTimeSlotLessonIndexes) {
                    lessonPenalties[lessonIndex] += conflictTimeSlotPenaltyPerLesson;
                }
            }

            if (timeWindowCount > 0) {
                timeWindowCount = Math.pow(timeWindowCount, academicGroupTimeWindowPenaltyPower);

                double timeWindowPenalty = timeWindowCount * academicGroupTimeWindowPenalty;
                penalty += timeWindowPenalty;

                double timeWindowPenaltyPerLesson = timeWindowPenalty / timeWindowLessonIndexes.size();
                for (int lessonIndex : timeWindowLessonIndexes) {
                    lessonPenalties[lessonIndex] += timeWindowPenaltyPerLesson;
                }
            }
        }

        for (Entry<Integer, Set<Integer>> placeLessonsEntry : placeLessonsMap.entrySet()) {
            int placeIndex = placeLessonsEntry.getKey();
            Set<Integer> lessonIndexes = placeLessonsEntry.getValue();

            double conflictTimeSlotsCount = 0;

            Iterator<Integer> lessonIndexIterator = lessonIndexes.iterator();
            Lesson prevLesson = null;
            List<Integer> conflictTimeSlotLessonIndexes = new ArrayList<>();
            while (lessonIndexIterator.hasNext()) {
                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();

                        if (lesson.getDay() == prevLesson.getDay()) {
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                        conflictTimeSlotLessonIndexes.add(lessonIndex);
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                    conflictTimeSlotLessonIndexes.add(lessonIndex);
                                }
                            }
                        } else {
                            isNewDay = true;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, placeConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * placeConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;

                double conflictTimeSlotPenaltyPerLesson = conflictTimeSlotPenalty / conflictTimeSlotLessonIndexes.size();
                for (int lessonIndex : conflictTimeSlotLessonIndexes) {
                    lessonPenalties[lessonIndex] += conflictTimeSlotPenaltyPerLesson;
                }
            }
        }

//        return new EvaluatedTimetable(lessons, penalty, lessonPenalties, lecturerLessonsMap, academicGroupLessonsMap, placeLessonsMap);
        return new EvaluatedTimetable(lessons, penalty, lessonPenalties);
    }

    private void crossover(TimetableRequirements timetableRequirements, List<EvaluatedTimetable> population) {
        int crossoversCount = (int) Math.round(crossoverRate * population.size());
        for (int i = 0; i < crossoversCount; ++i) {
            int parentIndex1 = random.nextInt(population.size());
            int parentIndex2 = random.nextInt(population.size());
            while (parentIndex1 == parentIndex2) {
                parentIndex2 = random.nextInt(population.size());
            }

            EvaluatedTimetable parent1 = population.get(parentIndex1);
            EvaluatedTimetable parent2 = population.get(parentIndex1);

            CrossoverResult crossoverResult = crossover(timetableRequirements, parent1, parent2);

            population.add(crossoverResult.child1);
            population.add(crossoverResult.child2);
        }
    }

    private CrossoverResult crossover(TimetableRequirements timetableRequirements, EvaluatedTimetable parent1, EvaluatedTimetable parent2) {
        Lesson[] lessons1 = parent1.getLessons();
        Lesson[] lessons2 = parent2.getLessons();

        double[] lessonPenalties1 = parent1.getLessonPenalties();
        double[] lessonPenalties2 = parent2.getLessonPenalties();

        Comparator<Integer> parent1LessonPenaltiesComparator = (lessonIndex1, lessonIndex2) ->
                Double.compare(lessonPenalties1[lessonIndex2], lessonPenalties1[lessonIndex1]);

        Comparator<Integer> parent2LessonPenaltiesComparator = (lessonIndex1, lessonIndex2) ->
                Double.compare(lessonPenalties2[lessonIndex2], lessonPenalties2[lessonIndex1]);

        int lessonsCount = lessons1.length;

        Integer[] parent1LessonIndexes = new Integer[lessonsCount];
        Integer[] parent2LessonIndexes = new Integer[lessonsCount];
        for (int i = 0; i < parent1LessonIndexes.length; ++i) {
            parent1LessonIndexes[i] = i;
            parent2LessonIndexes[i] = i;
        }
        Arrays.sort(parent1LessonIndexes, parent1LessonPenaltiesComparator);
        Arrays.sort(parent2LessonIndexes, parent2LessonPenaltiesComparator);

        int crossoverPointMinValue = (int) Math.round(crossoverMinBadGeneRate * lessonsCount);
        int crossoverPointMaxValue = (int) Math.round(crossoverMaxBadGeneRate * lessonsCount);
        int crossoverPoint = crossoverPointMinValue + random.nextInt(crossoverPointMaxValue - crossoverPointMinValue + 1);

        Lesson[] childLessons1 = new Lesson[lessonsCount];
        Lesson[] childLessons2 = new Lesson[lessonsCount];

        boolean[] lessonIndexMarker = new boolean[lessonsCount];
        for (int i = 0; i < crossoverPoint; ++i) {
            int parent1LessonIndex = parent1LessonIndexes[i];
            int parent2LessonIndex = parent2LessonIndexes[i];

            lessonIndexMarker[parent1LessonIndex] = true;
            lessonIndexMarker[parent2LessonIndex] = true;
        }


        for (int i = 0; i < lessonsCount; ++i) {
            if (lessonIndexMarker[i]) {
                childLessons1[i] = lessons1[i].clone();
                childLessons2[i] = lessons2[i].clone();
            } else {
                childLessons1[i] = lessons2[i].clone();
                childLessons2[i] = lessons1[i].clone();
            }
        }

        EvaluatedTimetable child1 = evaluateTimetable(timetableRequirements, childLessons1);
        EvaluatedTimetable child2 = evaluateTimetable(timetableRequirements, childLessons2);

        return new CrossoverResult(child1, child2);
    }

    private void mutation(TimetableRequirements timetableRequirements, List<EvaluatedTimetable> population) {
        List<EvaluatedTimetable> mutatedPopulation = new ArrayList<>();
        List<LessonTimeSlot> timeSlots = timetableRequirements.getTimeSlots();
        List<LessonPlace> lessonPlaces = timetableRequirements.getLessonPlaces();

        population.forEach(timetable -> {
            boolean isTimetableMutated = false;

            double[] lessonPenalties = timetable.getLessonPenalties();

            Comparator<Integer> lessonPenaltiesComparator = (lessonIndex1, lessonIndex2) ->
                    Double.compare(lessonPenalties[lessonIndex2], lessonPenalties[lessonIndex1]);

            Lesson[] lessons = Arrays.stream(timetable.getLessons()).map(Lesson::clone).toArray(Lesson[]::new);
            int lessonsCount = lessons.length;

            Integer[] lessonIndexes = new Integer[lessonsCount];
            for (int i = 0; i < lessonIndexes.length; ++i) {
                lessonIndexes[i] = i;
            }
            Arrays.sort(lessonIndexes, lessonPenaltiesComparator);

            int worstLessonsCount = (int) Math.round(mutationBadGenesRate * lessonsCount);
            for (int i = 0; i < worstLessonsCount; ++i) {
                Integer lessonIndex = lessonIndexes[i];
                if (lessonPenalties[lessonIndex] == 0) {
                    break;
                }

                Lesson lesson = lessons[lessonIndex];
                if (random.nextDouble() <= mutationDayRate) {
                    Day day = getRandomDay();
                    if (day != lesson.getDay()) {
                        lesson.setDay(day);

                        isTimetableMutated = true;
                    }
                }

                if (random.nextDouble() <= mutationTimeSlotRate) {
                    LessonTimeSlot timeSlot = getRandomTimeSlot(timeSlots);
                    if (timeSlot != lesson.getTimeSlot()) {
                        lesson.setTimeSlot(timeSlot);

                        isTimetableMutated = true;
                    }
                }

                if (random.nextDouble() <= mutationPlaceRate) {
                    int lessonPlaceIndex = getRandomPlace(lessonPlaces);
                    if (lessonPlaceIndex != lesson.getPlaceIndex()) {
                        lesson.setPlaceIndex(lessonPlaceIndex);

                        isTimetableMutated = true;
                    }
                }

                LessonPeriodicity periodicity = lesson.getPeriodicity();
                if (periodicity != LessonPeriodicity.WEEKLY) {
                    if (random.nextDouble() <= mutationPeriodicityRate) {
                        lesson.setPeriodicity(periodicity == LessonPeriodicity.NUMERATOR ? LessonPeriodicity.DENOMINATOR : LessonPeriodicity.NUMERATOR);

                        isTimetableMutated = true;
                    }
                }
            }

            if (isTimetableMutated) {
                EvaluatedTimetable mutatedTimetable = evaluateTimetable(timetableRequirements, lessons);
                mutatedPopulation.add(mutatedTimetable);
            }
        });

        population.addAll(mutatedPopulation);
    }

    private List<EvaluatedTimetable> repair(TimetableRequirements timetableRequirements, List<EvaluatedTimetable> population) {
//        List<EvaluatedTimetable> repairedTimetables = new ArrayList<>(population.size());
//        for (EvaluatedTimetable timetable: population) {
//            repairedTimetables.add(repair(timetableRequirements, timetable));
//        }
//        return repairedTimetables;
        return population.stream().map(timetable -> repair(timetableRequirements, timetable)).collect(Collectors.toList());
    }

    private EvaluatedTimetable repair(TimetableRequirements timetableRequirements, EvaluatedTimetable timetable) {
        double prevPenalty = timetable.getPenalty();

        List<LessonRequirements> lessonRequirementsList = timetableRequirements.getLessonRequirementsList();

        Lesson[] lessons = timetable.getLessons();
        double[] lessonPenalties = timetable.getLessonPenalties();
        Map<Integer, Set<Integer>> lecturerLessonsMap = timetable.getLecturerLessonsMap();
        Map<Integer, Set<Integer>> academicGroupLessonsMap = timetable.getAcademicGroupLessonsMap();
        Map<Integer, Set<Integer>> placeLessonsMap = timetable.getPlaceLessonsMap();

        Comparator<Integer> lessonPenaltiesComparator = (lessonIndex1, lessonIndex2) ->
                Double.compare(lessonPenalties[lessonIndex2], lessonPenalties[lessonIndex1]);

        int lessonsCount = lessons.length;

        Integer[] lessonIndexes = new Integer[lessonsCount];
        for (int i = 0; i < lessonIndexes.length; ++i) {
            lessonIndexes[i] = i;
        }
        Arrays.sort(lessonIndexes, lessonPenaltiesComparator);

        for (int lessonIndex : lessonIndexes) {
            if (lessonPenalties[lessonIndex] == 0) {
                break;
            }

            Lesson lesson = lessons[lessonIndex];
            LessonRequirements lessonRequirements = lessonRequirementsList.get(lesson.getRequirementsIndex());

            int lecturerIndex = lessonRequirements.getLecturerIndex();
            Set<Integer> academicGroupIndexes = lessonRequirements.getAcademicGroupIndexes();
            int placeIndex = lesson.getPlaceIndex();

            Set<Integer> lecturerLessonIndexes = lecturerLessonsMap.get(lecturerIndex);
            List<Set<Integer>> academicGroupLessonIndexesList = academicGroupIndexes.stream()
                    .map(academicGroupIndex -> academicGroupLessonsMap.get(academicGroupIndex))
                    .collect(Collectors.toList());
            Set<Integer> placeLessonIndexes = placeLessonsMap.get(placeIndex);

            repair(timetableRequirements, lessons, lessonIndex, lecturerLessonIndexes, academicGroupLessonIndexesList,
                    placeLessonIndexes);
        }

        EvaluatedTimetable repairedTimetable = evaluateTimetable(timetableRequirements, lessons);

        double penalty = repairedTimetable.getPenalty();
        if (penalty > prevPenalty) {
            System.out.println("prevPenalty: " + prevPenalty + ", penalty: " + penalty);
        }

        return repairedTimetable;
    }

    private void repair(TimetableRequirements timetableRequirements, Lesson[] lessons, int lessonIndex,
                        Set<Integer> lecturerLessonIndexes, List<Set<Integer>> academicGroupLessonIndexesList,
                        Set<Integer> placeLessonIndexes) {

        Lesson lesson = lessons[lessonIndex];

        List<LessonTimeSlot> timeSlots = timetableRequirements.getTimeSlots();

        Day validDay = lesson.getDay();
        LessonTimeSlot validTimeSlot = lesson.getTimeSlot();

        double minPenalty = Double.MAX_VALUE;
        Day minDay = Day.MONDAY;
        LessonTimeSlot minTimeSlot = timeSlots.get(0);

        for (Day day : days) {
            for (LessonTimeSlot timeSlot : timeSlots) {
                lecturerLessonIndexes.remove(lessonIndex);
                academicGroupLessonIndexesList.forEach(academicGroupLessonIndexes -> {
                    academicGroupLessonIndexes.remove(lessonIndex);
                });
                placeLessonIndexes.remove(lessonIndex);

                lesson.setDay(day);
                lesson.setTimeSlot(timeSlot);

                lecturerLessonIndexes.add(lessonIndex);
                academicGroupLessonIndexesList.forEach(academicGroupLessonIndexes -> {
                    academicGroupLessonIndexes.add(lessonIndex);
                });
                placeLessonIndexes.add(lessonIndex);

                double penalty = calcLocalPenalty(lecturerLessonIndexes, academicGroupLessonIndexesList,
                        placeLessonIndexes, lessons);

                if (penalty < minPenalty) {
                    minDay = day;
                    minTimeSlot = timeSlot;

                    minPenalty = penalty;
                }
            }
        }

        lecturerLessonIndexes.remove(lessonIndex);
        academicGroupLessonIndexesList.forEach(academicGroupLessonIndexes -> {
            academicGroupLessonIndexes.remove(lessonIndex);
        });
        placeLessonIndexes.remove(lessonIndex);

        lesson.setDay(minDay);
        lesson.setTimeSlot(minTimeSlot);
//        lesson.setDay(validDay);
//        lesson.setTimeSlot(validTimeSlot);
//        if (minDay != validDay || minTimeSlot != validTimeSlot) {
//            System.out.println(String.format("minDay = %s, validDay = %s, minTimeSlot = %s, validTimeSlot = %s", minDay, validDay, minTimeSlot, validTimeSlot));
//        }

        lecturerLessonIndexes.add(lessonIndex);
        academicGroupLessonIndexesList.forEach(academicGroupLessonIndexes -> {
            academicGroupLessonIndexes.add(lessonIndex);
        });
        placeLessonIndexes.add(lessonIndex);
    }

    private List<EvaluatedTimetable> selection(List<EvaluatedTimetable> population) {
        population.sort(TIMETABLE_COMPARATOR);
        return population.subList(0, populationInitialSize);
    }

    private double calcLocalPenalty(Set<Integer> lecturerLessonIndexes, List<Set<Integer>> academicGroupLessonIndexesList,
                                    Set<Integer> placeLessonIndexes, Lesson[] lessons) {
        double penalty = 0;

        {
            double conflictTimeSlotsCount = 0;
            double timeWindowCount = 0;

            Iterator<Integer> lessonIndexIterator = lecturerLessonIndexes.iterator();
            Lesson prevLesson = null;
            while (lessonIndexIterator.hasNext()) {
                double timeWindowDayCount = 0;

                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        if (lesson.getDay() == prevLesson.getDay()) {
                            LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                }
                            }

                            if (lessonTimeSlotUtil.isWindow(prevLessonTimeSlot, lessonTimeSlot)) {
                                ++timeWindowDayCount;
                            }
                        } else {
                            isNewDay = true;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }

                if (timeWindowDayCount > 0) {
                    timeWindowDayCount = Math.pow(timeWindowDayCount, lecturerTimeWindowPenaltyDayPower);

                    timeWindowCount += timeWindowDayCount;
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, lecturerConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * lecturerConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;
            }

            if (timeWindowCount > 0) {
                timeWindowCount = Math.pow(timeWindowCount, lecturerTimeWindowPenaltyPower);

                double timeWindowPenalty = timeWindowCount * lecturerTimeWindowPenalty;
                penalty += timeWindowPenalty;
            }
        }

        for (Set<Integer> lessonIndexes : academicGroupLessonIndexesList) {
            double conflictTimeSlotsCount = 0;
            double timeWindowCount = 0;

            Iterator<Integer> lessonIndexIterator = lessonIndexes.iterator();
            Lesson prevLesson = null;
            while (lessonIndexIterator.hasNext()) {
                double timeWindowDayCount = 0;

                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        if (lesson.getDay() == prevLesson.getDay()) {
                            LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                }
                            }

                            if (lessonTimeSlotUtil.isWindow(prevLessonTimeSlot, lessonTimeSlot)) {
                                ++timeWindowDayCount;
                            }
                        } else {
                            isNewDay = true;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }

                if (timeWindowDayCount > 0) {
                    timeWindowDayCount = Math.pow(timeWindowDayCount, academicGroupTimeWindowPenaltyDayPower);

                    timeWindowCount += timeWindowDayCount;
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, academicGroupConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * academicGroupConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;
            }

            if (timeWindowCount > 0) {
                timeWindowCount = Math.pow(timeWindowCount, academicGroupTimeWindowPenaltyPower);

                double timeWindowPenalty = timeWindowCount * academicGroupTimeWindowPenalty;
                penalty += timeWindowPenalty;
            }
        }

        {
            double conflictTimeSlotsCount = 0;

            Iterator<Integer> lessonIndexIterator = placeLessonIndexes.iterator();
            Lesson prevLesson = null;
            while (lessonIndexIterator.hasNext()) {
                while (lessonIndexIterator.hasNext()) {
                    int lessonIndex = lessonIndexIterator.next();
                    Lesson lesson = lessons[lessonIndex];
                    LessonTimeSlot lessonTimeSlot = lesson.getTimeSlot();
                    LessonPeriodicity lessonPeriodicity = lesson.getPeriodicity();

                    boolean isNewDay = false;
                    if (prevLesson != null) {
                        LessonTimeSlot prevLessonTimeSlot = prevLesson.getTimeSlot();

                        if (lesson.getDay() == prevLesson.getDay()) {
                            LessonPeriodicity prevLessonPeriodicity = prevLesson.getPeriodicity();

                            if (lessonTimeSlotUtil.isConflict(prevLessonTimeSlot, lessonTimeSlot)) {
                                if (lessonPeriodicity == prevLessonPeriodicity) {
                                    if (lessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                        ++conflictTimeSlotsCount;
                                    } else {
                                        conflictTimeSlotsCount += 0.5;
                                    }
                                } else if (lessonPeriodicity == LessonPeriodicity.WEEKLY || prevLessonPeriodicity == LessonPeriodicity.WEEKLY) {
                                    conflictTimeSlotsCount += 0.5;
                                }
                            }
                        } else {
                            isNewDay = true;
                        }
                    }

                    prevLesson = lesson;
                    if (isNewDay) {
                        break;
                    }
                }
            }

            if (conflictTimeSlotsCount > 0) {
                conflictTimeSlotsCount = Math.pow(conflictTimeSlotsCount, placeConflictTimeSlotPenaltyPower);

                double conflictTimeSlotPenalty = conflictTimeSlotsCount * placeConflictTimeSlotPenalty;
                penalty += conflictTimeSlotPenalty;
            }
        }

        return penalty;
    }

    public void printTimetable(TimetableRequirements timetableRequirements, EvaluatedTimetable timetable) {
        for (Lesson lesson : timetable.getLessons()) {
            System.out.printf("%3d | %9s | %s | %3d | %s \n",
                    lesson.getRequirementsIndex(),
                    lesson.getDay(),
                    lesson.getTimeSlot(),
                    lesson.getPlaceIndex(),
                    lesson.getPeriodicity()
            );
        }
    }

    private LessonPeriodicity getRandomLessonPeriodicity(double numberOfClassesPerWeek) {
        if (numberOfClassesPerWeek >= 1) {
            return LessonPeriodicity.WEEKLY;
        }

        return getRandomLessonPeriodicity();
    }

    private Day getRandomDay() {
        return days[random.nextInt(days.length)];
    }

    private LessonTimeSlot getRandomTimeSlot(List<LessonTimeSlot> timeSlots) {
        return timeSlots.get(random.nextInt(timeSlots.size()));
    }

    private int getRandomPlace(List<LessonPlace> lessonPlaces) {
        return random.nextInt(lessonPlaces.size());
    }

    private LessonPeriodicity getRandomLessonPeriodicity() {
        return random.nextBoolean() ? LessonPeriodicity.NUMERATOR : LessonPeriodicity.DENOMINATOR;
    }
}

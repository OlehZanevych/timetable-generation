package org.lnu.timetable.generation;

import lombok.AllArgsConstructor;
import org.lnu.timetable.generation.service.TimetableGenerationDemo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@AllArgsConstructor
@SpringBootApplication
public class TimetableGenerationApp implements CommandLineRunner {

    private final TimetableGenerationDemo timetableGenerationDemo;

    public static void main(String[] args) {
        SpringApplication.run(TimetableGenerationApp.class, args);
    }

    @Override
    public void run(String... args) {
//        timetableGenerationDemo.generateTimetableRequirementsExample1();
//        timetableGenerationDemo.generateTimetableRequirementsExample2();
//        timetableGenerationDemo.generateTimetableRequirementsExample3();
//        timetableGenerationDemo.generateTimetableRequirementsExample4();

        timetableGenerationDemo.timetableGenerationExample1();
//        timetableGenerationDemo.timetableGenerationExample2();
//        timetableGenerationDemo.timetableGenerationExample3();
//        timetableGenerationDemo.timetableGenerationExample4();
    }
}

package com.campuscore;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Optional sample data for a first demonstration. It runs only when app.demo-data=true
 * (environment variable APP_DEMO_DATA=true) and only when no section exists yet.
 * Sample logins: students use their roll number as the password, faculty use their faculty ID.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private final DepartmentRepository departments;
    private final AcademicService academic;
    private final AdminService admin;
    private final AssignmentService assignments;

    public DemoDataSeeder(DepartmentRepository departments, AcademicService academic,
                          AdminService admin, AssignmentService assignments) {
        this.departments = departments;
        this.academic = academic;
        this.admin = admin;
        this.assignments = assignments;
    }

    @Override
    public void run(String... args) {
        Department cse = departments.findByCodeIgnoreCase("CSE").orElse(null);
        Department ece = departments.findByCodeIgnoreCase("ECE").orElse(null);
        if (cse == null || ece == null || !academic.allSections().isEmpty()) return;

        Section cse2a = academic.createSection(cse.getId(), 2, "A");
        academic.createSection(cse.getId(), 2, "B");
        Section ece1a = academic.createSection(ece.getId(), 1, "A");
        academic.createSection(ece.getId(), 3, "A");   // a section with no students

        Subject ds = academic.createSubject("CS201", "Data Structures", cse.getId());
        Subject db = academic.createSubject("CS202", "Database Systems", cse.getId());
        Subject circuits = academic.createSubject("EC101", "Basic Electronics", ece.getId());

        admin.createFaculty("FAC001", "Meera Nair", cse.getId(), "Associate Professor");
        admin.createFaculty("FAC002", "Arjun Rao", ece.getId(), "Assistant Professor");

        admin.createStudent("CSE2A01", "Anita Reddy", cse2a.getId(), 88.0, 8.7);
        admin.createStudent("CSE2A02", "Kiran Kumar", cse2a.getId(), 68.5, 7.1);
        admin.createStudent("CSE2A03", "Divya Sharma", cse2a.getId(), 52.0, 5.4);
        admin.createStudent("ECE1A01", "Rahul Verma", ece1a.getId(), 79.0, 6.8);

        TeachingAssignment dsClass = academic.assignTeacher("FAC001", ds.getId(), cse2a.getId());
        academic.assignTeacher("FAC001", db.getId(), cse2a.getId());
        academic.assignTeacher("FAC002", circuits.getId(), ece1a.getId());

        assignments.create("FAC001", dsClass.getId(), "Linked list implementation",
            "Implement a singly linked list with insert, delete and search. Submit the work in class.",
            LocalDate.now().plusDays(7));
    }
}

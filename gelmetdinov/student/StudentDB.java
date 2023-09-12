package info.kgeorgiy.ja.gelmetdinov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {
    
    //:note: UPPER_CASE
    private static final Comparator<Student> studentComparator = Comparator.comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparing(Student::getId);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return get(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return get(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return get(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return get(students, student -> student.getFirstName() + " " + student.getLastName());

    }

    private static <T> List<T> get(List<Student> students, Function<Student, T> predicate) {
        return students.stream().map(predicate).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream().map(Student::getFirstName).sorted(String::compareTo)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Student::compareTo)
                .map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Student::compareTo)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return findBy(students, (student) -> true);
    }

    //:note: final var
    //:note: compare exclude null
    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, final String name) {
        return findBy(students, (student -> student.getFirstName().equals(name)));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, final String name) {
        return findBy(students, (student -> student.getLastName().equals(name)));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, final GroupName group) {
        return findBy(students, (student -> student.getGroup().equals(group)));
    }

    private static List<Student> findBy(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate)
                .sorted(studentComparator)
                .collect(Collectors.toList());
    }
    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> student.getGroup().equals(group))
                .collect(Collectors
                        .toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

}

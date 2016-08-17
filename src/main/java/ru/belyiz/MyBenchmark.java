/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package ru.belyiz;

import org.openjdk.jmh.annotations.*;

import java.awt.print.PrinterAbortException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@OperationsPerInvocation(1000)
@Fork(5)
public class MyBenchmark {

    @State(Scope.Benchmark)
    public static class Params {

        private static final int STRINGS_LIST_LENGTH = 100000;
        private static final int DOUBLES_LIST_LENGTH = 100000;
        private static final int OBJECTS_LIST_LENGTH = 100000;

        private List<String> stringsList;
        private List<Double> doublesList;
        private List<User> objectsList;
        private Set<User> objectsSet;

        private String[] strs = new String[]{"abcd1ea", "fghijak", "lmno1ap", "qrstau", "vwxyz1", "11124a", "asdadas1das", "asd3 21 fea", "asda1", "1"};

        @Setup
        public void setup() {
            stringsList = new ArrayList<>(STRINGS_LIST_LENGTH);
            doublesList = new ArrayList<>(DOUBLES_LIST_LENGTH);
            objectsList = new ArrayList<>(OBJECTS_LIST_LENGTH);
            objectsSet = new TreeSet<>();

            Random random = new Random();
            IntStream.range(0, STRINGS_LIST_LENGTH)
                    .forEach(i -> stringsList.add(strs[i % strs.length]));
            IntStream.range(0, DOUBLES_LIST_LENGTH)
                    .forEach(i -> doublesList.add(random.nextDouble()));
            IntStream.range(0, OBJECTS_LIST_LENGTH)
                    .forEach(i -> {
                                User user = new User(random.nextLong(), random.nextInt(100), random.nextInt(999999));
                                objectsList.add(user);
                                objectsSet.add(user);
                            }
                    );
        }

        public class User implements Comparable {

            private long id;
            private int age;
            private long count;

            public User(long id, int age, long count) {
                this.id = id;
                this.age = age;
                this.count = count;
            }

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }

            public int getAge() {
                return age;
            }

            public void setAge(int age) {
                this.age = age;
            }

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                User user = (User) o;
                return id == user.id;
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }

            @Override
            public String toString() {
                return "User{" +
                        "id=" + id +
                        ", age=" + age +
                        ", count=" + count +
                        '}';
            }

            @Override
            public int compareTo(Object o) {
                long count2 = ((User) o).getCount();
                if (count > count2) return 1;
                else if (count == count2) return 0;
                else return -1;
            }
        }
    }

    @Benchmark
    public List<String> simpleStringOperationsLoop(Params params) {
        List<String> newStrings = new ArrayList<>();
        for (String str : params.stringsList) {
            if (str.indexOf("1") > 3) {
                newStrings.add(str.toUpperCase().substring(0, 3));
            }
        }
        return newStrings;
    }

    @Benchmark
    public List<String> simpleStringOperationsStream(Params params) {
        return params.stringsList
                .stream()
                .filter(str -> str.indexOf("1") > 3)
                .map(str -> str.toUpperCase().substring(0, 3))
                .collect(Collectors.toList());
    }

    @Benchmark
    public List<String> simpleStringOperationsStreamParallel(Params params) {
        return params.stringsList
                .parallelStream()
                .filter(str -> str.indexOf("1") > 3)
                .map(str -> str.toUpperCase().substring(0, 3))
                .collect(Collectors.toList());
    }

    @Benchmark
    public long sumDoublesLoop(Params params) {
        long sum = 0;
        for (Double dbl : params.doublesList) {
            sum += (dbl * Integer.MAX_VALUE) / 1234567890;
        }
        return sum;
    }

    @Benchmark
    public long sumDoublesStream(Params params) {
        return params.doublesList.stream()
                .mapToLong(dbl -> (long) (dbl * Integer.MAX_VALUE) / 1234567890)
                .reduce(Long::sum)
                .orElse(0);
    }

    @Benchmark
    public long sumDoublesStreamParallel(Params params) {
        return params.doublesList.parallelStream()
                .mapToLong(dbl -> (long) (dbl * Integer.MAX_VALUE) / 1234567890)
                .reduce(Long::sum)
                .orElse(0);
    }

    @Benchmark
    public double minDoublesLoop(Params params) {
        double min = Double.MIN_VALUE;
        for (Double dbl : params.doublesList) {
            if (dbl < min) {
                min = dbl;
            }
        }
        return min;
    }

    @Benchmark
    public double minDoublesStream(Params params) {
        return params.doublesList
                .stream()
                .map(d -> Math.pow(Math.sqrt(d), 10))
                .reduce(Double::min)
                .orElse(Double.MIN_VALUE);
    }

    @Benchmark
    public double minDoublesStreamParallel(Params params) {
        return params.doublesList
                .parallelStream()
                .reduce(Double::min)
                .orElse(Double.MIN_VALUE);
    }

    @Benchmark
    public List<String> top10UsersListStream(Params params) {
        return params.objectsList
                .stream()
                .distinct()
                .sorted()
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public List<String> top10UsersListStreamParallel(Params params) {
        return params.objectsList
                .parallelStream()
                .distinct()
                .sorted()
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public List<String> top10UsersSetStream(Params params) {
        return params.objectsSet
                .stream()
                .distinct()
                .sorted()
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public List<String> top10UsersSetStreamParallel(Params params) {
        return params.objectsSet
                .parallelStream()
                .distinct()
                .sorted()
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public List<String> top10UsersFilteredStream(Params params) {
        return params.objectsList
                .stream()
                .filter(Objects::nonNull)
                .filter(u -> u.getAge() > 50)
                .distinct()
                .sorted((u1, u2) -> {
                    long count1 = u1.getCount();
                    long count2 = u2.getCount();
                    return count1 > count2 ? 1 : (count1 == count2 ? 0 : -1);
                })
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public List<String> top10UsersFilteredStreamParallel(Params params) {
        return params.objectsList
                .parallelStream()
                .filter(Objects::nonNull)
                .filter(u -> u.getAge() > 50)
                .distinct()
                .sorted((u1, u2) -> {
                    long count1 = u1.getCount();
                    long count2 = u2.getCount();
                    return count1 > count2 ? 1 : (count1 == count2 ? 0 : -1);
                })
                .limit(10)
                .map(Objects::toString)
                .collect(Collectors.toList());

    }

    @Benchmark
    public void limitStream(Params params) {
        params.doublesList
                .stream()
                .map(i -> {
                    return Math.pow(Math.log(Math.sqrt(i)), 15);
                })
                .filter(i -> {
                    return Math.exp(i) > Math.log(i);
                })
                .limit(100)
                .forEach(i -> i++);
    }

    @Benchmark
    public void limitStreamParallel(Params params) {
        params.doublesList
                .stream()
                .map(i -> {
                    return Math.pow(Math.log(Math.sqrt(i)), 15);
                })
                .filter(i -> {
                    return Math.exp(i) > Math.log(i);
                })
                .limit(100)
                .forEach(i -> i++);
    }
//
//    static class Person {
//        int age;
//
//        public int getAge() {
//            return age;
//        }
//    }
//
//    static int getCountByPerson(Person person) {
//        return 100;
//    }

//    public void i(Params params) {
//        List<Person> persons = new ArrayList<>();
//        long totalCount = 0;
//        for (Person person : persons) {
//            if (person == null) {
//                continue;
//            }
//            if (person.age > 30) {
//                int count = getCountByPerson(person);
//                if (count > 0) {
//                    totalCount += count;
//                }
//            }
//        }
//
//        long totalCount = persons
//                .stream()
//                .filter(Objects::nonNull)
//                .filter(person -> person.getAge() > 30)
//                .map(person -> getCountByPerson(person))
//                .filter(count -> count > 0)
//                .reduce(Integer::sum);
//    }
//
//    public static void main(String[] args) {
//        MyBenchmark myBenchmark = new MyBenchmark();
//        Params params = new Params();
//        params.setup();
//        myBenchmark.top10UsersSetStream(params);
//    }
}

//Benchmark                                               Mode  Samples   Score  Score error  Units
//r.b.MyBenchmark.minDoublesLoop                          avgt       50   9,909        0,160  us/op
//r.b.MyBenchmark.minDoublesStream                        avgt       50   1,641        0,041  us/op
//r.b.MyBenchmark.minDoublesStreamParallel                avgt       50   0,770        0,032  us/op

//r.b.MyBenchmark.simpleStringOperationsLoop              avgt       50  10,544        0,240  us/op
//r.b.MyBenchmark.simpleStringOperationsStream            avgt       50  10,807        0,229  us/op
//r.b.MyBenchmark.simpleStringOperationsStreamParallel    avgt       50   3,544        0,145  us/op

//r.b.MyBenchmark.sumDoublesLoop                          avgt       50   1,329        0,021  us/op
//r.b.MyBenchmark.sumDoublesStream                        avgt       50   0,761        0,013  us/op
//r.b.MyBenchmark.sumDoublesStreamParallel                avgt       50   0,700        0,020  us/op


//Benchmark                                  | Mode | Samples |   Score |  Score error |  Units
//-------------------------------------------|------|---------|---------|--------------|--------
//r.b.MyBenchmark.top10UsersStreamParallel   | avgt |      50 | 701,111 |       16,788 |  us/op
//-------------------------------------------|------|---------|---------|--------------|--------
//r.b.MyBenchmark.top10UsersStream           | avgt |      50 | 693,266 |       15,162 |  us/op

//100
//Benchmark                                   Mode  Samples  Score  Score error  Units
//r.b.MyBenchmark.top10UsersListStream            avgt       50  0,323        0,006  us/op
//r.b.MyBenchmark.top10UsersListStreamParallel    avgt       50  1,038        0,019  us/op
//r.b.MyBenchmark.top10UsersSetStream             avgt       50  0,047        0,002  us/op
//r.b.MyBenchmark.top10UsersSetStreamParallel     avgt       50  0,782        0,034  us/op

//100000
//r.b.MyBenchmark.top10UsersListStream            avgt       50  701,111       16,788  us/op
//r.b.MyBenchmark.top10UsersListStreamParallel    avgt       50  525,240       15,149  us/op
//r.b.MyBenchmark.top10UsersSetStream             avgt       50    0,044        0,001  us/op
//r.b.MyBenchmark.top10UsersSetStreamParallel     avgt       50    8,064        0,250  us/op

//r.b.MyBenchmark.limitStream            avgt       50  39,940        0,812  us/op
//r.b.MyBenchmark.limitStreamParallel    avgt       50  40,508        0,641  us/op
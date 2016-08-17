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
}
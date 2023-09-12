package info.kgeorgiy.ja.gelmetdinov.arrayset;

import java.util.*;

public class ArraySet<T> implements NavigableSet<T> {
    private final List<T> arrayList;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        arrayList = new ArrayList<>();
        comparator = null;
    }

    public ArraySet(Comparator<? super T> comparator) {
        arrayList = new ArrayList<>();
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {
        Set<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        arrayList = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    @Override
    public String toString() {
        return arrayList.toString();
    }

    @Override
    public T lower(T t) {
        int index = binSearch(t);
        return index == 0 ? null : arrayList.get(index - 1);
    }

    @Override
    public T floor(T t) {
        int index = binSearch(t);
        if (size() == 0 || isNull(size() + 1, -1, first(), t, 1)) {
            return null;
        }
        if (index < size()) {
            int delta = (compare(arrayList.get(index), t) == 0) ? 0 : -1;
            return arrayList.get(index + delta);
        }
        return last();
    }

    @Override
    public T higher(T t) {
        int index = binSearch(t);
        if (size() == 0 || isNull(size() + 1, index, t, last(), 0)) {
            return null;
        }
        return (compare(arrayList.get(index), t) == 0) ? arrayList.get(index + 1) : arrayList.get(index);
    }

    @Override
    public T ceiling(T t) {
        int index = binSearch(t);
        return (size() == 0 || isNull(size() + 1, -1, t, last(), 1))
                ? null : arrayList.get(index);
    }

    private boolean isNull(int lessThanOrEqual, int moreThanOrEqual, T leftCompare, T rightCompare, int result) {
        return size() >= lessThanOrEqual
                || size() <= moreThanOrEqual || compare(leftCompare, rightCompare) == result;
    }

    @Override
    public int size() {
        return arrayList.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(arrayList, (T) o, comparator) >= 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public Object[] toArray() {
        return arrayList.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return arrayList.toArray(a);
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(arrayList).listIterator();
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        ReversedList<T> reversedList = new ReversedList<>(arrayList);
        reversedList.reverse();
        return new ArraySet<>(reversedList, Collections.reverseOrder(comparator));
    }


    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }


    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Error: the first element should be less than the second");
        }

        int binSearchFrom = binSearch(fromElement);
        int binSearchTo = binSearch(toElement);
        if (binSearchFrom >= size()) {
            return new ArraySet<>();
        }
        int fromIncl = compare(arrayList.get(binSearchFrom), fromElement) == 0 && !fromInclusive ? 1 : 0;
        int toIncl = 0;
        if (binSearchTo < size()) {
            toIncl = compare(arrayList.get(binSearchTo), toElement) == 0 && toInclusive ? 1 : 0;
        }

        if (binSearchFrom + fromIncl > binSearchTo + toIncl || size() == 0) {
            return new ArraySet<>();
        }
        return new ArraySet<>(arrayList.subList(binSearchFrom + fromIncl, binSearchTo + toIncl));
    }

    @SuppressWarnings("unchecked")
    private int compare(T fromElement, T toElement) {
        return comparator != null ? comparator.compare(fromElement, toElement)
                : ((Comparable<T>) fromElement).compareTo(toElement);
    }


    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (isEmpty() || compare(first(), toElement) > 0) {
            return new ArraySet<>();
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (isEmpty() || compare(last(), fromElement) < 0) {
            return new ArraySet<>();
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);

    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
        return arrayList.get(0);
    }

    @Override
    public T last() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
        return arrayList.get(arrayList.size() - 1);
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private int binSearch(T element) {
        int normalized = Collections.binarySearch(arrayList, element, comparator);
        if (normalized < 0) {
            normalized = -normalized - 1;
        }
        return normalized;
    }

    static class ReversedList<E> extends AbstractList<E> implements RandomAccess {
        private final List<E> reversedList;
        private boolean isReversed;

        private ReversedList(List<E> data) {
            this.reversedList = data;
        }

        @Override
        public E get(int index) {
            return isReversed ? reversedList.get(size() - 1 - index) : reversedList.get(index);
        }

        @Override
        public int size() {
            return reversedList.size();
        }

        public void reverse() {
            isReversed = !isReversed;
        }
    }

}

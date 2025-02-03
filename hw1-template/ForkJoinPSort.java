//UT-EID=

import java.util.*;
import java.util.concurrent.*;

public class ForkJoinPSort {
    /* Notes:
     * The input array (A) is also the output array,
     * The range to be sorted extends from index begin, inclusive, to index end, exclusive,
     * Sort in increasing order when increasing=true, and decreasing order when increasing=false,
     */
    public static void parallelSort(int[] A, int begin, int end, boolean increasing) {
        // Simple Insertion sort when array is less than or equal to 16
        if (end - begin <= 16) {
            insertionSort(A, begin, end, increasing);
            return;
        }

        if (begin >= end) {
            return;
        }

        int pivot = A[end - 1];
        int leftPointer = begin;
        int rightPointer = end - 1;

        while (leftPointer < rightPointer) {
            if (increasing) {
                while (A[leftPointer] <= pivot && leftPointer < rightPointer) {
                    leftPointer++;
                }
                while (A[rightPointer] >= pivot && leftPointer < rightPointer) {
                    rightPointer--;
                }
            } else {
                while (A[leftPointer] >= pivot && leftPointer < rightPointer) {
                    leftPointer++;
                }
                while (A[rightPointer] <= pivot && leftPointer < rightPointer) {
                    rightPointer--;
                }
            }
            swap(A, leftPointer, rightPointer);
        }

        swap(A, leftPointer, end - 1);

        ForkJoinPool.commonPool().invoke(new SortTask(A, begin, leftPointer, increasing));
        ForkJoinPool.commonPool().invoke(new SortTask(A, leftPointer + 1, end, increasing));
    }

    private static void insertionSort(int[] A, int begin, int end, boolean increasing) {
        for (int i = begin + 1; i < end; i++) {
            int key = A[i];
            int j = i - 1;

            if (increasing) {
                while (j >= begin && A[j] > key) {
                    A[j + 1] = A[j];
                    j--;
                }
            } else {
                while (j >= begin && A[j] < key) {
                    A[j + 1] = A[j];
                    j--;
                }
            }
            A[j + 1] = key;
        }
    }

    private static void swap(int[] array, int index1, int index2) {
        int temp = array[index1];
        array[index1] = array[index2];
        array[index2] = temp;
    }

    private static class SortTask extends RecursiveAction {
        private final int[] A;
        private final int begin;
        private final int end;
        private final boolean increasing;

        SortTask(int[] A, int begin, int end, boolean increasing) {
            this.A = A;
            this.begin = begin;
            this.end = end;
            this.increasing = increasing;
        }

        @Override
        protected void compute() {
            parallelSort(A, begin, end, increasing);
        }
    }

    public static void main(String[] args) {
        int[] array1 = {5, 2, 9, 1, 5, 6};
        int[] array2 = {3, 0, -2, 5, -1, 4, 1};
        int[] array3 = {10, 7, 8, 9, 1, 5};

        System.out.println("Original array1: " + Arrays.toString(array1));
        parallelSort(array1, 0, array1.length, true);
        System.out.println("Sorted array1 (ascending): " + Arrays.toString(array1));

        System.out.println("Original array2: " + Arrays.toString(array2));
        parallelSort(array2, 0, array2.length, false);
        System.out.println("Sorted array2 (descending): " + Arrays.toString(array2));

        System.out.println("Original array3: " + Arrays.toString(array3));
        parallelSort(array3, 0, array3.length, true);
        System.out.println("Sorted array3 (ascending): " + Arrays.toString(array3));
    }
}
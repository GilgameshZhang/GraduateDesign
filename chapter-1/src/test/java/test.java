import java.util.HashSet;
import java.util.Set;

public class test {
        public static void main(String[] args) {
            int[] numbers = {6, 58, 77, 44, 57, 25, 59, 53, 60, 74, 39, 3, 65, 70, 22, 78, 67, 19, 40, 31, 15, 26, 7, 0, 12, 21, 71, 32, 48, 63, 2, 27, 69, 64, 41, 37, 66, 50, 35, 45, 1, 52, 73, 54, 79, 51, 36, 11, 18, 38, 24, 56, 17, 5, 62, 75, 20, 28, 4, 29, 16, 8, 33, 23, 72, 49, 30, 55, 61, 43, 13, 14, 47, 68, 9, 42, 34, 10, 76, 46};
            Set<Integer> seen = new HashSet<>();
            boolean hasDuplicates = false;

            for (int number : numbers) {
                if (!seen.add(number)) {
                    hasDuplicates = true;
                    break;
                }
            }

            if (hasDuplicates) {
                System.out.println("The array contains duplicate numbers.");
            } else {
                System.out.println("The array does not contain duplicate numbers.");
            }
        }
}

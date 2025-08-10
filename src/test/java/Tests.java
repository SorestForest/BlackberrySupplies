import java.util.Objects;
import java.util.Scanner;

public class Tests {


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String a = sc.nextLine();
        String b = sc.nextLine();
        System.out.println(Objects.equals(a, b));
        System.out.println(Objects.equals(null, b));
        System.out.println(b == null);
        System.out.println();
    }
}

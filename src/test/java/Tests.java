import ru.SorestForest.Settings;

import java.util.Scanner;

public class Tests {


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        for (int i = 0; i < n; i++) {
            int hours, minutes, lh, lm, rh, rm;
            hours = sc.nextInt();
            minutes = sc.nextInt();
            lh = sc.nextInt();
            lm = sc.nextInt();
            rh = sc.nextInt();
            rm = sc.nextInt();
            System.out.format("%d %d %d %d %d %d\n", hours, minutes, lh, lm, rh, rm);
            System.out.println(Settings.isBetween(hours, minutes, lh, lm, rh, rm));
        }
    }
}

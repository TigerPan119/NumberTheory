import java.math.BigInteger;
import java.util.Arrays;

public class HillCracker {

    //invert a 2x2 matrix (mod 26)
    public static int[][] invert(int[][] M) {
        //find determinant (modded down)
        int a = M[0][0], b = M[0][1], c = M[1][0], d = M[1][1];
        int det = (a * d - b * c);
        det = (((det) % 26 + 26) % 26);
        if(gcd(det,26)!=1) {
            return null; //not invertible
        }
        //find mod inverse of determinant
        int detInv = modInverse(det,26);
        //compute adjugate and multiply by mod inv to get inverted matrix
        int[][] inv = new int[2][2];
        inv[0][0] = (detInv * d) % 26;
        inv[0][1] = (detInv * ((-b + 26) % 26)) % 26;
        inv[1][0] = (detInv * ((-c + 26) % 26)) % 26;
        inv[1][1] = (detInv * a) % 26;
        return inv;
    }

    //algorithms to convert letters and index number back and forth (A = 0)
    public static int letterToNum(char c) {
        return c - 'A';
    }
    public static char numToLetter(int n) {
        return (char)('A' + ((n % 26 + 26) % 26));
    }

    //Calculate the mod inverse of a (mod m)
    public static int modInverse(int a, int m) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger M = BigInteger.valueOf(m);
        return A.modInverse(M).intValue();
    }

    //Calculate the gcd of a and b
    public static int gcd(int a, int b) {
        BigInteger A = BigInteger.valueOf(a);
        BigInteger B = BigInteger.valueOf(b);
        return A.gcd(B).intValue();
    }

    //multiply 2 2x2 matrices
    public static int[][] multiply(int[][] a, int[][] b) {
        int[][] result = new int[2][2];
        result[0][0] = a[0][0]*b[0][0] + a[0][1]*b[1][0];
        result[0][0] = result[0][0] % 26;
        result[0][1] = a[0][0]*b[0][1] + a[0][1]*b[1][1];
        result[0][1] = result[0][1] % 26;
        result[1][0] = a[1][0]*b[0][0] + a[1][1]*b[1][0];
        result[1][0] = result[1][0] % 26;
        result[1][1] = a[1][0]*b[0][1] + a[1][1]*b[1][1];
        result[1][1] = result[1][1] % 26;
        return result;
    }

    //build matrix from digraphs
    public static int[][] buildMatrix(String d1, String d2) {
        int[][] M = new int[2][2];
        M[0][0] = letterToNum(d1.charAt(0)); M[1][0] = letterToNum(d1.charAt(1));
        M[0][1] = letterToNum(d2.charAt(0)); M[1][1] = letterToNum(d2.charAt(1));
        return M;
    }

    //score a text, see how much it looks like conventional English language
    public static int score(String s) {
        int score = 0;
        String[] commons = {
            "THE", "AND", "TO", "OF", "A", "IN", "IS", "IT", "THAT", "HAVE", "ING", "ED"
        };
        for (String t: commons) {
            if (s.contains(t)) score += 5;
        }
        return score;
    }

    //multiply 2x2 by 2x1 matrix
    public static int[] multiply(int[][] a, int[] b) {
        int[] r = new int[2];
        r[0] = (a[0][0]*b[0] + a[0][1]*b[1]) % 26;
        r[1] = (a[1][0]*b[0] + a[1][1]*b[1]) % 26;
        if (r[0]<0) r[0]+=26;
        if (r[1]<0) r[1]+=26;
        return r;
    }

    //decrypt the code using using Kinv (x = Kinv*y)
    public static String decrypt(String code, int[][] Kinv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < code.length(); i += 2) {
            int[] y = new int[] { letterToNum(code.charAt(i)), letterToNum(code.charAt(i+1)) };
            int[] x = multiply(Kinv, y);
            sb.append(numToLetter(x[0])).append(numToLetter(x[1]));
        }
        return sb.toString();
    }

    //the crazy method, crack: c = code, tc = common digraphs
    public static void crack(String c, String[] tc) {
        String code = c;
        String[] topCode = tc; //Most common digraphs from the code
        String[] topLang = {
            "TH","HE","IN","ER","AN","RE","ND","AT","ON","NT",
            "HA","ES","ST","EN","ED","TO","IT","OU","EA","HI",
            "IS","OR","TI","AS","TE","ET","NG","OF","AL","DE",
            "SE","LE","SA","SI","AR","VE","RA","LD","UR"
        }; //Most common digraphs from the English language
        int nc = topCode.length;
        int nl = topLang.length;
        int bestScore = -1;
        String bestText = null;
        int[][] bestK = null;
        for(int i = 0; i < nc; i++) for(int j = 0; j < nc; j++) if(i != j) {
            //Hill encrypted y = Kx, y is the encrypted code, x is the original text, K is the key
            int[][] y = buildMatrix(topCode[i], topCode[j]);
            for(int a = 0; a < nl; a++) for(int b = 0; b < nl; b++) if(a != b){
                int[][] x = buildMatrix(topLang[a], topLang[b]);
                //y = Kx constructed, not get K by K = y(x_inv)
                int[][] xinv = invert(x);
                if(xinv==null) continue;
                int[][] K = multiply(y, xinv);
                //Got K, now invert K so that we can decrypt the code using K inverse
                int detK = (K[0][0]*K[1][1] - K[0][1]*K[1][0]) % 26;
                detK = (detK + 26) % 26;
                if(gcd(detK, 26) != 1) continue;
                int[][] Kinv = invert(K);
                if(Kinv == null) continue;
                //Got Kinv, now decrypt and score the result
                String result = decrypt(code, Kinv);
                int score = score(result);
                if (score > bestScore){
                    bestScore = score;
                    bestText = result;
                    bestK = K;
                    System.out.println(
                        "New best score: " + bestScore + " with mapping " + topCode[i] 
                        + "," + topCode[j] + " -> " + topLang[a] + "," + topLang[b] + 
                        "  Key K = [" + K[0][0] + "," + K[0][1] + ";" + K[1][0] + "," 
                        + K[1][1] + "]"
                    );
                    System.out.println("Decrypted Text: " + result);
                    System.out.println();
                }
            }
        }
        System.out.println("Best score: " + bestScore);
        System.out.println("K = " + Arrays.deepToString(bestK));
        System.out.println("Decrypted Text: " + bestText);
    }

    public static void main(String[] args) {
        crack("GSDGLTOUJOLTQPKJKHBTNXPNYDKRWTSQQCYTOEQTHQJZGOQRYDQRNSLTTJTVEXGRHUUZRBDTFVZLSVIWKJORTV", 
            new String[] {"LT", "KJ", "YD", "QR", "TV", "GS"});
    }
}

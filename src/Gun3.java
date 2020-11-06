// JavaOpt: -Xms120m -Xmx120m
// JaveVer: 11

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Gun3 {

    static float[] charLM;
    static double[] modelScores;

    static int LINE_BUF_SIZE = 65 * 2 * 2;

    static double[] XStd = {14.89, 18.81, 19.56, 18.07, 19.13, 20.92, 18.05, 17.86, 18.56, 17.82, 19.42, 14.12, 19.83, 18.53, 19.38, 18.66, 18.30, 21.39, 19.01, 18.83, 19.44, 24.72, 19.16, 19.00, 17.83, 19.59};

    static double[] YStd = {23.58, 22.49, 20.64, 20.76, 22.93, 19.67, 20.33, 20.77, 23.39, 22.42, 22.48, 24.03, 21.96, 22.31, 22.56, 23.68, 25.26, 20.85, 21.09, 21.13, 22.91, 25.41, 22.89, 21.53, 21.54, 22.51};

    static int[][] aanLetterEdge = {
            {0, 167, 519, 348, 83, 433},
            {591, 697, 348, 177, 644, 262},
            {379, 485, 348, 177, 432, 262},
            {273, 379, 519, 348, 326, 433},
            {221, 327, 699, 519, 274, 609},
            {379, 485, 519, 348, 432, 433},
            {485, 591, 519, 348, 538, 433},
            {591, 697, 519, 348, 644, 433},
            {751, 857, 699, 519, 804, 609},
            {697, 803, 519, 348, 750, 433},
            {803, 909, 519, 348, 856, 433},
            {909, 1080, 519, 348, 994, 433},
            {803, 909, 348, 177, 856, 262},
            {697, 803, 348, 177, 750, 262},
            {857, 963, 699, 519, 910, 609},
            {963, 1080, 699, 519, 1021, 609},
            {0, 115, 699, 519, 57, 609},
            {327, 433, 699, 519, 380, 609},
            {167, 273, 519, 348, 220, 433},
            {433, 539, 699, 519, 486, 609},
            {645, 751, 699, 519, 698, 609},
            {485, 591, 348, 177, 538, 262},
            {115, 221, 699, 519, 168, 609},
            {273, 379, 348, 177, 326, 262},
            {539, 645, 699, 519, 592, 609},
            {167, 273, 348, 177, 220, 262}
    };

    static char[][] nearLettersMore = new char[26][26];

    static double[][] gaussianX = new double[26][300];
    static double[][] gaussianY = new double[26][300];

    static double[][] posCharScores;

    static String preInput = " ";

    static class Pair {
        double score;
        String better;
        int num;

        public Pair(double score, String better) {
            this.score = score;
            this.better = better;
        }
    }

    static class ScoreHolder {
        double[] chars;
        double score;

        public ScoreHolder() {
            this.score = 0;
            this.chars = new double[63];
        }

        ScoreHolder copy() {
            ScoreHolder cln = new ScoreHolder();
            cln.score = this.score;
            System.arraycopy(this.chars, 0, cln.chars, 0, this.chars.length);
            return cln;
        }
    }

    static class Holder {

        ScoreHolder lm;
        ScoreHolder pos;

        double score;

        private Holder() {
        }

        public Holder(ScoreHolder lm, ScoreHolder pos) {
            this.lm = lm;
            this.pos = pos;
            caleScore();
        }

        void caleScore() {
            score = pos.score + lm.score * 4.5;
        }

        Holder copy() {
            Holder cln = new Holder();
            cln.score = this.score;
            cln.lm = this.lm.copy();
            cln.pos = this.pos.copy();
            return cln;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

//        Thread.sleep(15000);

        long start = System.currentTimeMillis();

        buildNearLetters();

        buildGaussianXs();

        buildModelScores(args[0]);

        byte[] buffer = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.allocate(LINE_BUF_SIZE);

        short[] lineBuffer;

        BufferedInputStream reader = new BufferedInputStream(System.in);
        int c, pos = 0;
        do {
            while ((c = reader.read(buffer, 0, 2)) >= 0) {
                if (buffer[0] == 0 && buffer[1] == 0 && (pos = byteBuffer.position()) % 4 == 0) {
                    break;
                } else {
                    byteBuffer.put(buffer);
                }
            }

            if (pos == 0) {
                break;
            }

            lineBuffer = new short[pos / 2];
            byteBuffer.rewind().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lineBuffer);
            pos = 0;

            char[][] nearLetters;
//            if (lineBuffer.length > 15 * 2) {
//                nearLetters = nearLettersLess;
//            } else {
            nearLetters = nearLettersMore;
//            }
            String result = exhaustiveSearch(nearLetters, lineBuffer);
            // System.out.println(result);

        } while (c > 0);

        long end = System.currentTimeMillis();
        System.out.println((end - start) / 1000);
    }

    static void buildModelScores(String path) throws IOException {

        byte[] bytes = Files.readAllBytes(Path.of(path));

        charLM = new float[bytes.length / 4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(charLM);
        byteBuffer.clear();

        modelScores = new double[charLM.length];
        for (int i = 0; i < charLM.length; i++) {
            if (charLM[i] <= 0.0)
                modelScores[i] = -1000.0;
            else
                modelScores[i] = Math.log(charLM[i]);
        }
    }

    static void buildNearLetters() {
        for (int i = 0; i < aanLetterEdge.length; i++) {
            int idxMore = 0;
            nearLettersMore[i][idxMore++] = (char) ('a' + i);
            for (int j = 0; j < aanLetterEdge.length; j++) {
                if (i == j) {
                    continue;
                }
                int distance = calcDistance(aanLetterEdge[i][4], aanLetterEdge[i][5],
                        aanLetterEdge[j][4], aanLetterEdge[j][5]);

                char c = (char) ('a' + j);

                if (distance < 80000) {
                    nearLettersMore[i][idxMore++] = c;
                }
            }
        }
    }

    static void buildGaussianXs() {
        for (int i = 0; i < 26; i++) {
            for (int j = 0; j < 300; j++) {
                gaussianX[i][j] = calculateGaussian(XStd[i], j);
                gaussianY[i][j] = calculateGaussian(YStd[i], j);
            }
        }
    }

    static String exhaustiveSearch(char[][] nearLetters, short[] pPosition) {

        String input = getInputStr(pPosition);

        char[] chars = input.toCharArray();

        int nInputLen = chars.length;

        posCharScores = new double[26][26];

        int k = nInputLen < 4 ? nInputLen : 4;

        Pair best = exhaustiveSearch_dp(nearLetters, pPosition, k, chars);

        preInput = input;

        return best.better;
    }

    static Pair exhaustiveSearch_dp(char[][] nearLetters, short[] pPosition, int k, char[] buf) {

        char[][] build = new char[buf.length][14];

        List<Pair> cache = new ArrayList<>();

        int id = 0;
        for (int i = buf.length - 1; i >= 0; i--) {
            int idx = buf[i] - 'a';
            build[id] = nearLetters[idx];
        }

        char[] cr = new char[1];
        char[] last = build[build.length - 1];
        for (int i = 0; i < last.length; i++) {
//            if ()
            cr[0] = last[i];
//            if () {
//
//            }
            double score = getLMScoreByChar(modelScores, cr, 0) * 4.5 + getLogPosScoreByChar(pPosition, last[i], 0);

            Pair pair = new Pair(score, String.valueOf(last[i]));
            if (i == 0) {
                pair.num = 1;
            }
            cache.add(pair);
        }

        int f = 0;
        Pair best = new Pair(- 99999999, "");
        for (int i = build.length - 1; i >= 0 ; i--) {
            List<Pair> temp = new ArrayList<>();
            for (int j = 0; j < build[i].length; j++) {
                for (Pair pair : cache) {
                    if (j == 0 && pair.num >= k) {
                        continue;
                    }
                    char neo = build[i][j];
                    if (neo == 0) {
                        continue;
                    }
                    char[] crx = (pair.better + neo).toCharArray();
                    double score = getLMScoreByChar(modelScores, crx, f) * 4.5 + getLogPosScoreByChar(pPosition, neo, f);
                    Pair m = new Pair(score + pair.score, pair.better + neo);
                    m.num = pair.num;
                    if (j == 0) {
                        m.num += 1;
                    }
                    if (i == 0) {
                        if (m.score > best.score) {
                            best.score = m.score;
                            best.better = m.better;
                        }
                    } else {
                        temp.add(m);
                    }
                }
            }
            cache.clear();
            cache = temp;
            f++;
        }

        return best;
    }

    static void exhaustiveSearch_i(char[][] nearLetters, short[] pPosition, int p, int k, char[] buf, Pair best, Holder holder) {
        if (k == 0 || p == pPosition.length) {
            if (holder.score > best.score) {
                best.score = holder.score;
                best.better = String.valueOf(buf);
            }
        } else {
            if (holder.score > best.score) {
                best.score = holder.score;
                best.better = String.valueOf(buf);
            }
            for (; p < pPosition.length; p += 2) {
                int cIdx = p / 2;
                char bak = buf[cIdx];
                int idx = bak - 'a';
                char[] nears = nearLetters[idx];
                for (int i = 0; i < nears.length; i++) {
                    if (nears[i] == '0') {
                        break;
                    }
                    if (filterByDistance(pPosition[p], pPosition[p + 1], nears[i])) {
                        continue;
                    }
                    buf[cIdx] = nears[i];
                    changeScore(modelScores, pPosition, buf, holder, cIdx);
                    exhaustiveSearch_i(nearLetters, pPosition, p + 2, k - 1, buf, best, holder);
                }
                buf[cIdx] = bak;
                changeScore(modelScores, pPosition, buf, holder, cIdx);
            }
        }
    }

    static boolean filterByDistance(short x1, short y1, char newCh) {
        int idx = newCh - 'a';
        int x2 = aanLetterEdge[idx][4];
        int y2 = aanLetterEdge[idx][5];
        return calcDistance(x1, y1, x2, y2) > 50000;
    }

    static int calcDistance(int x1, int y1, int x2, int y2) {
        return (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    }

    static void changeScore(double[] modelScore, short[] pPosition, char[] str, Holder holder, int idx) {
        changeLogLMScore(modelScore, str, holder.lm, idx);
        changeLogPosScore(pPosition, str, holder.pos, idx);
        holder.caleScore();
    }

    static Holder getScore(double[] modelScore, short[] pPosition, char[] str) {
        return new Holder(getLogLMScore(modelScore, str), getLogPosScore(pPosition, str));
    }

    static void changeLogLMScore(double[] modelScore, char[] szCor, ScoreHolder scoreHolder, int idx) {

        int nLen = szCor.length;

        for (int i = idx; i < idx + 4; ++i) {
            if (i >= nLen)
                break;
            double charScore = getLMScoreByChar(modelScore, szCor, i);
            scoreHolder.score = scoreHolder.score - scoreHolder.chars[i] + charScore;
            scoreHolder.chars[i] = charScore;
        }
    }

    static ScoreHolder getLogLMScore(double[] modelScore, char[] szCor) {

        ScoreHolder scoreHolder = new ScoreHolder();
        double score = 0.0;

        int nLen = szCor.length;

        for (int i = 0; i < nLen; ++i) {
            double charScore = getLMScoreByChar(modelScore, szCor, i);
            scoreHolder.chars[i] = charScore;
            score += charScore;
        }
        scoreHolder.score = score;
        return scoreHolder;
    }

    static double getLMScoreByChar(double[] modelScore, char[] szCor, int i) {
        int idx;
        switch (i) {
            case 0:
                idx = szCor[i] - '`';
                break;
            case 1:
                idx = (szCor[i - 1] - '`') * 27;
                idx += szCor[i] - '`';
                break;
            case 2:
                idx = (szCor[i - 2] - '`') * (27 * 27);
                idx += (szCor[i - 1] - '`') * 27;
                idx += szCor[i] - '`';
                break;
            default:
                idx = (szCor[i - 3] - '`') * (27 * 27 * 27);
                idx += (szCor[i - 2] - '`') * (27 * 27);
                idx += (szCor[i - 1] - '`') * 27;
                idx += szCor[i] - '`';
                break;
        }
        return modelScore[idx];
    }

    static void changeLogPosScore(short[] p_pPosition, char[] szCor, ScoreHolder scoreHolder, int idx) {
        double charScore = getLogPosScoreByChar(p_pPosition, szCor[idx], idx);
        scoreHolder.score = scoreHolder.score - scoreHolder.chars[idx] + charScore;
        scoreHolder.chars[idx] = charScore;
    }

    static ScoreHolder getLogPosScore(short[] p_pPosition, char[] szCor) {

        ScoreHolder scoreHolder = new ScoreHolder();
        double score = 0.0;

        int nLen = szCor.length;
        for (int i = 0; i < nLen; i++) {
            double charScore = getLogPosScoreByChar(p_pPosition, szCor[i], i);
            scoreHolder.chars[i] = charScore;
            score += charScore;
        }
        scoreHolder.score = score;
        return scoreHolder;
    }

    static double getLogPosScoreByChar(short[] p_pPosition, char c, int i) {
        double score = 0.0;
        int idx = c - 'a';

        if (posCharScores[i][idx] != 0) {
            score = posCharScores[i][idx];
        } else {
            float sRelativeX = absToRelativeX(c, p_pPosition[i * 2]);
            float sRelativeY = absToRelativeY(c, p_pPosition[i * 2 + 1]);
            int sx = (int)Math.abs(sRelativeX);
            int sy = (int)Math.abs(sRelativeY);

            score += gaussianX[idx][sx];
            score += gaussianY[idx][sy];

            posCharScores[i][idx] = score;
        }
        return score;
    }

    static float absToRelativeX(char new_ch, short x_pos_abs) {
        int idx = new_ch - 'a';
        return (float) 100.0 * (x_pos_abs - aanLetterEdge[idx][4]) / (aanLetterEdge[idx][1] - aanLetterEdge[idx][0]);
    }

    static float absToRelativeY(char new_ch, short y_pos_abs) {
        int idx = new_ch - 'a';
        return (float) 150.0 * (y_pos_abs - aanLetterEdge[idx][5]) / (aanLetterEdge[idx][2] - aanLetterEdge[idx][3]);
    }

    static double calculateGaussian(double p_dSigma, float p_sPos) {
        double dX = p_sPos;
        double dPI = Math.PI;
        return -dX * dX / (2 * p_dSigma * p_dSigma) - Math.log(Math.sqrt(2 * dPI) * p_dSigma);
    }

    static String getInputStr(short[] pPosition) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i < pPosition.length) {
            builder.append(getCharByPosition(pPosition[i], pPosition[i + 1]));
            i += 2;
        }
        return builder.toString();
    }

    static char getCharByPosition(short p_usX, short p_usY) {
        for (int i = 0; i < 26; ++i) {
            if (aanLetterEdge[i][0] <= p_usX && p_usX <= aanLetterEdge[i][1] &&
                    aanLetterEdge[i][3] <= p_usY && p_usY <= aanLetterEdge[i][2]) {
                return (char) ('a' + i);
            }
        }
        return '`';
    }
}
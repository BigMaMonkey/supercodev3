package coder3Gun;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Gun3 {

    static float[] charLM;
    static int LINEBUFSIZE = 65 * 2 * 2;

    // 每个按键x坐标的标准差
    static double XStd[] = { 14.89, 18.81, 19.56, 18.07, 19.13, 20.92, 18.05, 17.86, 18.56, 17.82, 19.42, 14.12, 19.83, 18.53, 19.38, 18.66, 18.30, 21.39, 19.01, 18.83, 19.44, 24.72, 19.16, 19.00, 17.83, 19.59 };

// 每个按键y坐标的标准差
    static double YStd[] = { 23.58, 22.49, 20.64, 20.76, 22.93, 19.67, 20.33, 20.77, 23.39, 22.42, 22.48, 24.03, 21.96, 22.31, 22.56, 23.68, 25.26, 20.85, 21.09, 21.13, 22.91, 25.41, 22.89, 21.53, 21.54, 22.51 };


    static int aanLetterEdge[][] = {
        { 0, 167, 519, 348, 83, 433 },
        { 591, 697, 348, 177, 644, 262 },
        { 379, 485, 348, 177, 432, 262 },
        { 273, 379, 519, 348, 326, 433 },
        { 221, 327, 699, 519, 274, 609 },
        { 379, 485, 519, 348, 432, 433 },
        { 485, 591, 519, 348, 538, 433 },
        { 591, 697, 519, 348, 644, 433 },
        { 751, 857, 699, 519, 804, 609 },
        { 697, 803, 519, 348, 750, 433 },
        { 803, 909, 519, 348, 856, 433 },
        { 909, 1080, 519, 348, 994, 433 },
        { 803, 909, 348, 177, 856, 262 },
        { 697, 803, 348, 177, 750, 262 },
        { 857, 963, 699, 519, 910, 609 },
        { 963, 1080, 699, 519, 1021, 609 },
        { 0, 115, 699, 519, 57, 609 },
        { 327, 433, 699, 519, 380, 609 },
        { 167, 273, 519, 348, 220, 433 },
        { 433, 539, 699, 519, 486, 609 },
        { 645, 751, 699, 519, 698, 609 },
        { 485, 591, 348, 177, 538, 262 },
        { 115, 221, 699, 519, 168, 609 },
        { 273, 379, 348, 177, 326, 262 },
        { 539, 645, 699, 519, 592, 609 },
        { 167, 273, 348, 177, 220, 262 }
    };

    static class Pair {
        double score;
        String better;

        public Pair(double score, String better) {
            this.score = score;
            this.better = better;
        }
    }


    public static void main(String[] args) throws IOException {

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));

        charLM = new float[bytes.length / 4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(charLM);
        byteBuffer.clear();

        bytes = null; // null array for gc

        byte[] buffer = new byte[2];
        byteBuffer = ByteBuffer.allocate(LINEBUFSIZE);
        short[] lineBuffer;

        BufferedInputStream reader = new BufferedInputStream(System.in);
        int c = -1, pos = 0;
        do {
            while ((c = reader.read(buffer, 0, 2)) >= 0) {
                if (buffer[0] == 0 && buffer[1] == 0 && (pos = byteBuffer.position()) % 4 == 0) {
                    break;
                }
                else {
                    byteBuffer.put(buffer);
                }
            }

            if (pos == 0) {
                break;
            }

            lineBuffer = new short[pos / 2];
            byteBuffer.rewind().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lineBuffer);
            pos = 0;
            String result = exhaustiveSearch(lineBuffer);
            System.out.println(result);

        } while (c > 0);
    }

    static String exhaustiveSearch(short[] pPosition) {

        String input = getInputStr(pPosition);
        char[] chars = input.toCharArray();

        double score = getScore(charLM, pPosition, chars);

        int nInputLen = chars.length;

        int k = nInputLen < 4 ? nInputLen : 4; //k为最大纠错字符数

        Pair best = new Pair(score, input);

        exhaustiveSearch_i(pPosition, 0, k, chars, best);

        return best.better;
    }

    // 穷举搜索，i是当前字符序号，k是还剩几个可替换的字符
    static void exhaustiveSearch_i(short[] pPosition,
                            int p, int k, char[] buf, Pair best)
    {
        if (k == 0 || p == pPosition.length) {
            // 算分
            double score = getScore(charLM, pPosition, buf);
            if (score > best.score) {
                best.score = score;
                best.better = String.valueOf(buf);
            }
        } else {
            // 从当前位置开始不做纠错的分数
            double score = getScore(charLM, pPosition, buf);
            if (score > best.score) {
                best.score = score;
                best.better = String.valueOf(buf);
            }
            // 递归向下
            for(; p < pPosition.length; p += 2) {
                char bak = buf[p/2];
                for (char newCh = 'a'; newCh < bak; ++newCh) {
                    if (filterByDistance(pPosition[p], pPosition[p+1], newCh))
                        continue;
                    buf[p/2] = newCh;
                    exhaustiveSearch_i(pPosition, p+2, k-1, buf, best);
                }
                for (char newCh = (char) (bak + 1); newCh <= 'z'; ++newCh) {
                    if (filterByDistance(pPosition[p], pPosition[p+1], newCh))
                        continue;
                    buf[p/2] = newCh;
                    exhaustiveSearch_i(pPosition, p+2, k-1, buf, best);
                }
                buf[p/2] = bak;
            }
        }
    }


    //过滤距离超过一定值的按键
    static boolean filterByDistance(short x1, short y1, char newCh) {
//        int x2 = aanLetterEdge[newCh -'a'][4];
//        int y2 = aanLetterEdge[newCh -'a'][5];
//        int temp = (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1);
//        if (temp > 50000)
//            return true;
        return false;
    }

    static double getScore(float[] charLM, short[] pPosition, char[] str) {
        return getLogLMScore(charLM, str, str.length) * 4.5 + getLogPosScore(pPosition, str, str.length);
    }

    // 计算语言模型得分
    static double getLogLMScore(float[] model, char[] szCor, int nLen)
    {
        double score = 0.0;
        char[] szNewCor = new char[128];
        szNewCor[0] = '`';
        szNewCor[1] = '`';
        szNewCor[2] = '`';
        System.arraycopy(szCor,0, szNewCor ,3, nLen);

        for (int i=0; i< nLen; ++i)
        {
            float fLMScore = getLM(model, szNewCor[i], szNewCor[i + 1], szNewCor[i + 2], szNewCor[i + 3]);
            if (fLMScore <= 0.0)
                score += -1000.0;
            else
                score += Math.log(fLMScore);
        }
        return score;
    }

    // 计算位置得分
    static double getLogPosScore(short[] p_pPosition, char[] szCor, int nSize)
    {
        double score = 0.0;
        for (int i = 0; i < nSize; i++)
        {
            int nCharNo = szCor[i] - 'a';

            // 转换相对坐标
            float sRelativeX = absToRelativeX(szCor[i], p_pPosition[i * 2]);
            float sRelativeY = absToRelativeY(szCor[i], p_pPosition[i * 2 + 1]);

            score += calculateGaussian(XStd[nCharNo], sRelativeX);
            score += calculateGaussian(YStd[nCharNo], sRelativeY);
        }
        return score;
    }

    // 计算相对坐标
    static float absToRelativeX(char new_ch, short x_pos_abs) {
        int idx = new_ch - 'a';
        return (float) 100.0 * (x_pos_abs - aanLetterEdge[idx][4]) / (aanLetterEdge[idx][1] - aanLetterEdge[idx][0]);
    }

    static float absToRelativeY(char new_ch, short y_pos_abs) {
        int idx = new_ch - 'a';
        return (float)150.0 * (y_pos_abs - aanLetterEdge[idx][5]) / (aanLetterEdge[idx][2] - aanLetterEdge[idx][3]);
    }

    // 计算高斯分布概率
    static double calculateGaussian(double p_dSigma, float p_sPos) {
        double dX = (double)p_sPos;
        double dPI = Math.PI;
        return - dX * dX / (2 * p_dSigma * p_dSigma) - Math.log(Math.sqrt(2 * dPI) * p_dSigma);
    }

    // 访问语言模型
    // model是一个27*27*27*27的float数组
    static float getLM(float[] model, char preCh1, char preCh2, char preCh3, char curCh)
    {
        int idx;
        idx = (preCh1 - '`')*(27*27*27);
        idx += (preCh2 - '`')*(27*27);
        idx += (preCh3 - '`')*27;
        idx += curCh - '`';
        return model[idx];
    }

    static String getInputStr(short[] pPosition) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i < pPosition.length)
        {
            builder.append(getCharByPosition(pPosition[i], pPosition[i + 1]));
            i += 2;
        }
        return builder.toString();
    }

    // 通过横纵坐标获取对应字符
    static char getCharByPosition(short p_usX, short p_usY)
    {
        for (int i = 0; i < 26; ++i)
        {
            if (aanLetterEdge[i][0] <= p_usX && p_usX <= aanLetterEdge[i][1] &&
                    aanLetterEdge[i][3] <= p_usY && p_usY <= aanLetterEdge[i][2])
            {
                return (char)('a' + i);
            }
        }
        // 防错，返回一个模糊字符
        return '`';
    }
}

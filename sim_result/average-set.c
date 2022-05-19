#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

int main(void) {
    FILE *fp; // FILE型構造体
    int peoplenum[] = {10, 50, 100};
    double filecmname[] = {0,4,9,16,25,36};
    char stringcmnum[256];
    char stringcmsqrtnum[256];
    char string1[] = "result-";
    char string2[] = "-";
    char string3[] = "ave-";
    char string4[] = ".txt";

    int i,j;

    char avefname[256];
    char stringevacunum[10];

    // 配列サイズ（繰り返し回数）
    int patternsizepeople = sizeof peoplenum / sizeof peoplenum[0];
    int patternsizecm = sizeof filecmname / sizeof filecmname[0];

    // 避難者数のパターンごと
    for (j = 0 ; j < patternsizepeople ; j ++) {

        sprintf(stringevacunum,"%d", peoplenum[j]);

        //make file name ave-?.txt
        strcpy(avefname, string3);
        strcat(avefname, stringevacunum);
        strcat(avefname, string4);

        // 通信機台数のパターンごと
        for (i = 0 ; i < patternsizecm ; i++) {
            char fname[20];
            int fintname = filecmname[i];

            double cmsqrtnum;
            int cmsqrtintnum;
            char stringcmsqrtintnum[256];

            cmsqrtnum = sqrt(filecmname[i]);
            cmsqrtintnum = cmsqrtnum;

            sprintf(stringcmsqrtintnum,"%d",cmsqrtintnum);
            sprintf(stringcmnum,"%d",fintname);

            //make file name result-?-?.txt
            strcpy(fname, string1);
            strcat(fname, stringcmnum);
            strcat(fname, string2);
            strcat(fname, stringevacunum);
            strcat(fname, string4);


            char str[16];
            float f1, ave, perc;
            int num, cm, noncheckcm;

            int sum = 0;
            int cmsum  = 0;
            int checkmax = -1;
            int checkcmmax;

            // データファイル展開
            // ファイルを開く。失敗するとNULLを返す
            fp = fopen(fname, "r");
            if(fp == NULL) {
                printf("%s file not open!\n", fname);
                return -1;
            }


            while(fscanf(fp, "%f %s %d %d %d", &f1, str, &num, &cm, &noncheckcm) != EOF) {
                if (num > checkmax) {
                    checkmax = num;
                    checkcmmax = cm;
                } else if (num <= checkmax) {
                    sum += checkmax;
                    cmsum += checkcmmax;
                    checkmax = num;
                    checkcmmax = cm;
                }
            }
            sum += checkmax;
            cmsum += checkcmmax;
            ave = (float)sum / 10; //average of successful people
            perc = (ave / (float)peoplenum[j])*100; //percentage(%)


            // ファイルを閉じる
            fclose(fp);

            //open average file
            fp = fopen(avefname, "a");


            fprintf(fp, "%s %f\n",stringcmsqrtintnum , perc);

            //ファイルを閉じる
            fclose(fp);

        }
    }
    return 0;

}

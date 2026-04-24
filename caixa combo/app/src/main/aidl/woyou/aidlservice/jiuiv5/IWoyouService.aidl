// IWoyouService.aidl
package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;

interface IWoyouService {
    void printText(String text, ICallback callback);
    void printBarCode(String data, int symbology, int height, int width, int position, ICallback callback);
    void printQRCode(String data, int modulesize, int errorlevel, ICallback callback);
    void setAlignment(int alignment, ICallback callback);
    void setFontSize(float fontsize, ICallback callback);
    void setBold(boolean bold, ICallback callback);
    void lineWrap(int n, ICallback callback);
    void cutPaper(ICallback callback);
}

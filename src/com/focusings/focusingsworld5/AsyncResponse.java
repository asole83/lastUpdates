package com.focusings.focusingsworld5;

import java.util.List;

import com.focusings.focusingsworld5.ImageAndTextList.ImageAndText;

public interface AsyncResponse {
    void processFinish(List<ImageAndText> output,int tabNumber);
}
package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 签到热力图单日数据 VO.
 */
@Getter
@Setter
public class StreakDayVO {

    private LocalDate date;
    private Boolean hasReview;
    private Boolean hasCheckIn;
    private Integer pointsEarned;
}

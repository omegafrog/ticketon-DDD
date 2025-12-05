package org.codenbug.seat.domain;

import lombok.Getter;

/**
 * 대한민국 팔도 지역별 분류 enum
 */
@Getter
public enum RegionLocation {
    GYEONGGI("경기도", "경기"), SEOUL("서울특별시", "서울"), INCHEON("인천광역시", "인천"), BUSAN("부산광역시",
            "부산"), DAEGU("대구광역시", "대구"), DAEJEON("대전광역시", "대전"), GWANGJU("광주광역시",
                    "광주"), ULSAN("울산광역시", "울산"), SEJONG("세종특별자치시", "세종"), GANGWON("강원특별자치도",
                            "강원"), CHUNGBUK("충청북도", "충북"), CHUNGNAM("충청남도", "충남"), JEONBUK(
                                    "전북특별자치도", "전북"), JEONNAM("전라남도", "전남"), GYEONGBUK("경상북도",
                                            "경북"), GYEONGNAM("경상남도", "경남"), JEJU("제주특별자치도", "제주");

    private final String fullName;
    private final String shortName;

    RegionLocation(String fullName, String shortName) {
        this.fullName = fullName;
        this.shortName = shortName;
    }

    /**
     * 문자열로부터 RegionLocation을 찾는 메서드 전체 이름 또는 줄임 이름으로 매칭
     */
    public static RegionLocation fromString(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        String trimmed = location.trim();
        for (RegionLocation region : values()) {
            if (region.fullName.equals(trimmed) || region.shortName.equals(trimmed)) {
                return region;
            }
        }
        return null;
    }
}

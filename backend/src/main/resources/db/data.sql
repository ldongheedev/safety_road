-- ============================================================
-- SafeRoute 초기 데이터 - 성남시 분당구
-- 실행 전 init.sql로 테이블이 생성되어 있어야 합니다.
-- psql -U postgres -d "safety road" -f init.sql
-- psql -U postgres -d "safety road" -f data.sql
-- ============================================================

-- 기존 데이터 초기화
TRUNCATE safety_facility RESTART IDENTITY CASCADE;
TRUNCATE danger_zone RESTART IDENTITY CASCADE;

-- ============================================================
-- 1. CCTV (방범용 CCTV)
-- ============================================================
INSERT INTO safety_facility (facility_type, name, address, location, data_source) VALUES
-- 서현역 일대
('CCTV', '서현역 1번출구 CCTV', '성남시 분당구 서현동 255-2', ST_SetSRID(ST_MakePoint(127.1264, 37.3837), 4326), '성남시 공공데이터'),
('CCTV', '서현역 2번출구 CCTV', '성남시 분당구 서현동 255-5', ST_SetSRID(ST_MakePoint(127.1271, 37.3842), 4326), '성남시 공공데이터'),
('CCTV', '서현역 사거리 CCTV', '성남시 분당구 서현동 256', ST_SetSRID(ST_MakePoint(127.1258, 37.3830), 4326), '성남시 공공데이터'),
('CCTV', 'AK플라자 앞 CCTV', '성남시 분당구 서현동 267', ST_SetSRID(ST_MakePoint(127.1252, 37.3845), 4326), '성남시 공공데이터'),
('CCTV', '서현로 중앙 CCTV', '성남시 분당구 서현동 270', ST_SetSRID(ST_MakePoint(127.1240, 37.3850), 4326), '성남시 공공데이터'),
-- 야탑역 일대
('CCTV', '야탑역 1번출구 CCTV', '성남시 분당구 야탑동 514', ST_SetSRID(ST_MakePoint(127.1278, 37.4112), 4326), '성남시 공공데이터'),
('CCTV', '야탑역 3번출구 CCTV', '성남시 분당구 야탑동 515', ST_SetSRID(ST_MakePoint(127.1285, 37.4118), 4326), '성남시 공공데이터'),
('CCTV', '야탑역 사거리 CCTV', '성남시 분당구 야탑동 516', ST_SetSRID(ST_MakePoint(127.1272, 37.4105), 4326), '성남시 공공데이터'),
('CCTV', '탄천 야탑교 CCTV', '성남시 분당구 야탑동 520', ST_SetSRID(ST_MakePoint(127.1230, 37.4100), 4326), '성남시 공공데이터'),
-- 정자역 일대
('CCTV', '정자역 1번출구 CCTV', '성남시 분당구 정자동 158', ST_SetSRID(ST_MakePoint(127.1082, 37.3660), 4326), '성남시 공공데이터'),
('CCTV', '정자역 4번출구 CCTV', '성남시 분당구 정자동 159', ST_SetSRID(ST_MakePoint(127.1090, 37.3668), 4326), '성남시 공공데이터'),
('CCTV', 'NAVER 사옥 앞 CCTV', '성남시 분당구 정자동 178-1', ST_SetSRID(ST_MakePoint(127.1055, 37.3595), 4326), '성남시 공공데이터'),
('CCTV', '정자동 카페거리 CCTV', '성남시 분당구 정자동 160', ST_SetSRID(ST_MakePoint(127.1075, 37.3670), 4326), '성남시 공공데이터'),
-- 수내역 일대
('CCTV', '수내역 1번출구 CCTV', '성남시 분당구 수내동 44', ST_SetSRID(ST_MakePoint(127.1145, 37.3777), 4326), '성남시 공공데이터'),
('CCTV', '수내역 사거리 CCTV', '성남시 분당구 수내동 45', ST_SetSRID(ST_MakePoint(127.1150, 37.3770), 4326), '성남시 공공데이터'),
('CCTV', '수내동 로데오거리 CCTV', '성남시 분당구 수내동 46', ST_SetSRID(ST_MakePoint(127.1138, 37.3782), 4326), '성남시 공공데이터'),
-- 미금역 일대
('CCTV', '미금역 1번출구 CCTV', '성남시 분당구 구미동 175', ST_SetSRID(ST_MakePoint(127.1092, 37.3587), 4326), '성남시 공공데이터'),
('CCTV', '미금역 사거리 CCTV', '성남시 분당구 구미동 176', ST_SetSRID(ST_MakePoint(127.1085, 37.3580), 4326), '성남시 공공데이터'),
-- 오리역 일대
('CCTV', '오리역 1번출구 CCTV', '성남시 분당구 구미동 200', ST_SetSRID(ST_MakePoint(127.1095, 37.3393), 4326), '성남시 공공데이터'),
('CCTV', '오리역 사거리 CCTV', '성남시 분당구 구미동 201', ST_SetSRID(ST_MakePoint(127.1100, 37.3400), 4326), '성남시 공공데이터'),
-- 이매역 일대
('CCTV', '이매역 2번출구 CCTV', '성남시 분당구 이매동 120', ST_SetSRID(ST_MakePoint(127.1265, 37.3952), 4326), '성남시 공공데이터'),
('CCTV', '이매역 사거리 CCTV', '성남시 분당구 이매동 121', ST_SetSRID(ST_MakePoint(127.1258, 37.3945), 4326), '성남시 공공데이터'),
-- 판교역 일대
('CCTV', '판교역 1번출구 CCTV', '성남시 분당구 삼평동 670', ST_SetSRID(ST_MakePoint(127.1115, 37.3948), 4326), '성남시 공공데이터'),
('CCTV', '판교 테크노밸리 입구 CCTV', '성남시 분당구 삼평동 681', ST_SetSRID(ST_MakePoint(127.1070, 37.4020), 4326), '성남시 공공데이터'),
('CCTV', '판교역 사거리 CCTV', '성남시 분당구 삼평동 671', ST_SetSRID(ST_MakePoint(127.1120, 37.3955), 4326), '성남시 공공데이터'),
-- 탄천 산책로
('CCTV', '탄천 서현교 CCTV', '성남시 분당구 서현동 300', ST_SetSRID(ST_MakePoint(127.1190, 37.3835), 4326), '성남시 공공데이터'),
('CCTV', '탄천 수내교 CCTV', '성남시 분당구 수내동 80', ST_SetSRID(ST_MakePoint(127.1120, 37.3760), 4326), '성남시 공공데이터'),
('CCTV', '탄천 정자교 CCTV', '성남시 분당구 정자동 200', ST_SetSRID(ST_MakePoint(127.1050, 37.3650), 4326), '성남시 공공데이터'),
-- 주거지역 이면도로
('CCTV', '서현동 주택가 CCTV 1', '성남시 분당구 서현동 310', ST_SetSRID(ST_MakePoint(127.1300, 37.3860), 4326), '성남시 공공데이터'),
('CCTV', '서현동 주택가 CCTV 2', '성남시 분당구 서현동 315', ST_SetSRID(ST_MakePoint(127.1310, 37.3870), 4326), '성남시 공공데이터'),
('CCTV', '야탑동 주택가 CCTV', '성남시 분당구 야탑동 530', ST_SetSRID(ST_MakePoint(127.1320, 37.4130), 4326), '성남시 공공데이터'),
('CCTV', '정자동 아파트단지 CCTV', '성남시 분당구 정자동 170', ST_SetSRID(ST_MakePoint(127.1060, 37.3680), 4326), '성남시 공공데이터'),
('CCTV', '수내동 공원 CCTV', '성남시 분당구 수내동 60', ST_SetSRID(ST_MakePoint(127.1160, 37.3790), 4326), '성남시 공공데이터'),
('CCTV', '분당중앙공원 CCTV 1', '성남시 분당구 수내동 50', ST_SetSRID(ST_MakePoint(127.1175, 37.3800), 4326), '성남시 공공데이터'),
('CCTV', '분당중앙공원 CCTV 2', '성남시 분당구 수내동 51', ST_SetSRID(ST_MakePoint(127.1180, 37.3810), 4326), '성남시 공공데이터'),
('CCTV', '불곡산 입구 CCTV', '성남시 분당구 정자동 190', ST_SetSRID(ST_MakePoint(127.1020, 37.3720), 4326), '성남시 공공데이터'),
('CCTV', '율동공원 입구 CCTV', '성남시 분당구 구미동 220', ST_SetSRID(ST_MakePoint(127.1000, 37.3420), 4326), '성남시 공공데이터'),
('CCTV', '구미동 이면도로 CCTV', '성남시 분당구 구미동 185', ST_SetSRID(ST_MakePoint(127.1110, 37.3550), 4326), '성남시 공공데이터'),
('CCTV', '이매동 학교앞 CCTV', '성남시 분당구 이매동 130', ST_SetSRID(ST_MakePoint(127.1280, 37.3970), 4326), '성남시 공공데이터'),
('CCTV', '삼평동 주택가 CCTV', '성남시 분당구 삼평동 690', ST_SetSRID(ST_MakePoint(127.1080, 37.4000), 4326), '성남시 공공데이터');

-- ============================================================
-- 2. 가로등 (STREETLIGHT)
-- ============================================================
INSERT INTO safety_facility (facility_type, name, address, location, data_source) VALUES
-- 분당로 (남북 주도로)
('STREETLIGHT', '분당로 가로등 1', '성남시 분당구 서현동 260', ST_SetSRID(ST_MakePoint(127.1260, 37.3820), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 2', '성남시 분당구 서현동 262', ST_SetSRID(ST_MakePoint(127.1262, 37.3840), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 3', '성남시 분당구 서현동 264', ST_SetSRID(ST_MakePoint(127.1264, 37.3860), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 4', '성남시 분당구 이매동 100', ST_SetSRID(ST_MakePoint(127.1266, 37.3880), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 5', '성남시 분당구 이매동 105', ST_SetSRID(ST_MakePoint(127.1268, 37.3900), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 6', '성남시 분당구 이매동 110', ST_SetSRID(ST_MakePoint(127.1270, 37.3920), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 7', '성남시 분당구 야탑동 500', ST_SetSRID(ST_MakePoint(127.1275, 37.4060), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 8', '성남시 분당구 야탑동 505', ST_SetSRID(ST_MakePoint(127.1277, 37.4080), 4326), '성남시 공공데이터'),
('STREETLIGHT', '분당로 가로등 9', '성남시 분당구 야탑동 510', ST_SetSRID(ST_MakePoint(127.1280, 37.4100), 4326), '성남시 공공데이터'),
-- 수내로
('STREETLIGHT', '수내로 가로등 1', '성남시 분당구 수내동 30', ST_SetSRID(ST_MakePoint(127.1130, 37.3760), 4326), '성남시 공공데이터'),
('STREETLIGHT', '수내로 가로등 2', '성남시 분당구 수내동 32', ST_SetSRID(ST_MakePoint(127.1140, 37.3770), 4326), '성남시 공공데이터'),
('STREETLIGHT', '수내로 가로등 3', '성남시 분당구 수내동 34', ST_SetSRID(ST_MakePoint(127.1150, 37.3780), 4326), '성남시 공공데이터'),
('STREETLIGHT', '수내로 가로등 4', '성남시 분당구 수내동 36', ST_SetSRID(ST_MakePoint(127.1160, 37.3790), 4326), '성남시 공공데이터'),
-- 정자로
('STREETLIGHT', '정자로 가로등 1', '성남시 분당구 정자동 140', ST_SetSRID(ST_MakePoint(127.1060, 37.3640), 4326), '성남시 공공데이터'),
('STREETLIGHT', '정자로 가로등 2', '성남시 분당구 정자동 145', ST_SetSRID(ST_MakePoint(127.1070, 37.3650), 4326), '성남시 공공데이터'),
('STREETLIGHT', '정자로 가로등 3', '성남시 분당구 정자동 150', ST_SetSRID(ST_MakePoint(127.1080, 37.3660), 4326), '성남시 공공데이터'),
('STREETLIGHT', '정자로 가로등 4', '성남시 분당구 정자동 155', ST_SetSRID(ST_MakePoint(127.1090, 37.3670), 4326), '성남시 공공데이터'),
-- 서현로
('STREETLIGHT', '서현로 가로등 1', '성남시 분당구 서현동 230', ST_SetSRID(ST_MakePoint(127.1220, 37.3830), 4326), '성남시 공공데이터'),
('STREETLIGHT', '서현로 가로등 2', '성남시 분당구 서현동 235', ST_SetSRID(ST_MakePoint(127.1230, 37.3835), 4326), '성남시 공공데이터'),
('STREETLIGHT', '서현로 가로등 3', '성남시 분당구 서현동 240', ST_SetSRID(ST_MakePoint(127.1240, 37.3840), 4326), '성남시 공공데이터'),
('STREETLIGHT', '서현로 가로등 4', '성남시 분당구 서현동 245', ST_SetSRID(ST_MakePoint(127.1250, 37.3845), 4326), '성남시 공공데이터'),
-- 야탑로
('STREETLIGHT', '야탑로 가로등 1', '성남시 분당구 야탑동 480', ST_SetSRID(ST_MakePoint(127.1260, 37.4090), 4326), '성남시 공공데이터'),
('STREETLIGHT', '야탑로 가로등 2', '성남시 분당구 야탑동 485', ST_SetSRID(ST_MakePoint(127.1270, 37.4095), 4326), '성남시 공공데이터'),
('STREETLIGHT', '야탑로 가로등 3', '성남시 분당구 야탑동 490', ST_SetSRID(ST_MakePoint(127.1280, 37.4100), 4326), '성남시 공공데이터'),
('STREETLIGHT', '야탑로 가로등 4', '성남시 분당구 야탑동 495', ST_SetSRID(ST_MakePoint(127.1290, 37.4110), 4326), '성남시 공공데이터'),
-- 탄천 산책로
('STREETLIGHT', '탄천 산책로 가로등 1', '성남시 분당구 야탑동 550', ST_SetSRID(ST_MakePoint(127.1210, 37.4080), 4326), '성남시 공공데이터'),
('STREETLIGHT', '탄천 산책로 가로등 2', '성남시 분당구 서현동 290', ST_SetSRID(ST_MakePoint(127.1195, 37.3850), 4326), '성남시 공공데이터'),
('STREETLIGHT', '탄천 산책로 가로등 3', '성남시 분당구 수내동 70', ST_SetSRID(ST_MakePoint(127.1130, 37.3770), 4326), '성남시 공공데이터'),
('STREETLIGHT', '탄천 산책로 가로등 4', '성남시 분당구 정자동 195', ST_SetSRID(ST_MakePoint(127.1060, 37.3660), 4326), '성남시 공공데이터'),
('STREETLIGHT', '탄천 산책로 가로등 5', '성남시 분당구 구미동 210', ST_SetSRID(ST_MakePoint(127.1040, 37.3500), 4326), '성남시 공공데이터'),
-- 판교로
('STREETLIGHT', '판교로 가로등 1', '성남시 분당구 삼평동 660', ST_SetSRID(ST_MakePoint(127.1100, 37.3940), 4326), '성남시 공공데이터'),
('STREETLIGHT', '판교로 가로등 2', '성남시 분당구 삼평동 665', ST_SetSRID(ST_MakePoint(127.1110, 37.3950), 4326), '성남시 공공데이터'),
('STREETLIGHT', '판교로 가로등 3', '성남시 분당구 삼평동 670', ST_SetSRID(ST_MakePoint(127.1090, 37.3980), 4326), '성남시 공공데이터'),
('STREETLIGHT', '판교로 가로등 4', '성남시 분당구 삼평동 675', ST_SetSRID(ST_MakePoint(127.1080, 37.4000), 4326), '성남시 공공데이터'),
-- 미금로
('STREETLIGHT', '미금로 가로등 1', '성남시 분당구 구미동 170', ST_SetSRID(ST_MakePoint(127.1080, 37.3570), 4326), '성남시 공공데이터'),
('STREETLIGHT', '미금로 가로등 2', '성남시 분당구 구미동 172', ST_SetSRID(ST_MakePoint(127.1090, 37.3580), 4326), '성남시 공공데이터'),
('STREETLIGHT', '미금로 가로등 3', '성남시 분당구 구미동 174', ST_SetSRID(ST_MakePoint(127.1100, 37.3590), 4326), '성남시 공공데이터'),
-- 이면도로 (어두운 구간)
('STREETLIGHT', '서현동 이면도로 가로등', '성남시 분당구 서현동 320', ST_SetSRID(ST_MakePoint(127.1305, 37.3865), 4326), '성남시 공공데이터'),
('STREETLIGHT', '야탑동 이면도로 가로등', '성남시 분당구 야탑동 535', ST_SetSRID(ST_MakePoint(127.1315, 37.4125), 4326), '성남시 공공데이터'),
('STREETLIGHT', '정자동 이면도로 가로등', '성남시 분당구 정자동 175', ST_SetSRID(ST_MakePoint(127.1065, 37.3685), 4326), '성남시 공공데이터');

-- ============================================================
-- 3. 편의점 (CONVENIENCE_STORE)
-- ============================================================
INSERT INTO safety_facility (facility_type, name, address, location, data_source) VALUES
-- 서현역 근처
('CONVENIENCE_STORE', 'CU 서현역점', '성남시 분당구 서현동 255-10', ST_SetSRID(ST_MakePoint(127.1268, 37.3840), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'GS25 서현역점', '성남시 분당구 서현동 258', ST_SetSRID(ST_MakePoint(127.1255, 37.3835), 4326), '행정안전부'),
('CONVENIENCE_STORE', '세븐일레븐 서현로점', '성남시 분당구 서현동 240', ST_SetSRID(ST_MakePoint(127.1243, 37.3848), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'CU 서현AK점', '성남시 분당구 서현동 268', ST_SetSRID(ST_MakePoint(127.1250, 37.3850), 4326), '행정안전부'),
-- 야탑역 근처
('CONVENIENCE_STORE', 'GS25 야탑역점', '성남시 분당구 야탑동 514-3', ST_SetSRID(ST_MakePoint(127.1282, 37.4115), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'CU 야탑역점', '성남시 분당구 야탑동 518', ST_SetSRID(ST_MakePoint(127.1275, 37.4108), 4326), '행정안전부'),
('CONVENIENCE_STORE', '이마트24 야탑점', '성남시 분당구 야탑동 525', ST_SetSRID(ST_MakePoint(127.1290, 37.4120), 4326), '행정안전부'),
-- 정자역 근처
('CONVENIENCE_STORE', 'GS25 정자역점', '성남시 분당구 정자동 158-5', ST_SetSRID(ST_MakePoint(127.1085, 37.3665), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'CU 정자동점', '성남시 분당구 정자동 162', ST_SetSRID(ST_MakePoint(127.1078, 37.3672), 4326), '행정안전부'),
('CONVENIENCE_STORE', '세븐일레븐 정자점', '성남시 분당구 정자동 165', ST_SetSRID(ST_MakePoint(127.1095, 37.3658), 4326), '행정안전부'),
-- 수내역 근처
('CONVENIENCE_STORE', 'CU 수내역점', '성남시 분당구 수내동 44-3', ST_SetSRID(ST_MakePoint(127.1148, 37.3780), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'GS25 수내점', '성남시 분당구 수내동 48', ST_SetSRID(ST_MakePoint(127.1155, 37.3775), 4326), '행정안전부'),
-- 미금역 근처
('CONVENIENCE_STORE', 'CU 미금역점', '성남시 분당구 구미동 175-3', ST_SetSRID(ST_MakePoint(127.1095, 37.3590), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'GS25 미금점', '성남시 분당구 구미동 178', ST_SetSRID(ST_MakePoint(127.1088, 37.3583), 4326), '행정안전부'),
-- 오리역 근처
('CONVENIENCE_STORE', 'CU 오리역점', '성남시 분당구 구미동 200-5', ST_SetSRID(ST_MakePoint(127.1098, 37.3396), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'GS25 오리점', '성남시 분당구 구미동 205', ST_SetSRID(ST_MakePoint(127.1105, 37.3405), 4326), '행정안전부'),
-- 이매역 근처
('CONVENIENCE_STORE', 'CU 이매역점', '성남시 분당구 이매동 122', ST_SetSRID(ST_MakePoint(127.1270, 37.3955), 4326), '행정안전부'),
-- 판교역 근처
('CONVENIENCE_STORE', 'GS25 판교역점', '성남시 분당구 삼평동 672', ST_SetSRID(ST_MakePoint(127.1118, 37.3950), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'CU 판교테크노점', '성남시 분당구 삼평동 685', ST_SetSRID(ST_MakePoint(127.1075, 37.4015), 4326), '행정안전부'),
('CONVENIENCE_STORE', '세븐일레븐 판교점', '성남시 분당구 삼평동 688', ST_SetSRID(ST_MakePoint(127.1065, 37.4010), 4326), '행정안전부'),
-- 주거지역
('CONVENIENCE_STORE', 'CU 서현푸르지오점', '성남시 분당구 서현동 330', ST_SetSRID(ST_MakePoint(127.1295, 37.3875), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'GS25 야탑주공점', '성남시 분당구 야탑동 540', ST_SetSRID(ST_MakePoint(127.1310, 37.4135), 4326), '행정안전부'),
('CONVENIENCE_STORE', 'CU 정자한솔점', '성남시 분당구 정자동 180', ST_SetSRID(ST_MakePoint(127.1050, 37.3690), 4326), '행정안전부');

-- ============================================================
-- 4. 경찰서/치안센터 (POLICE)
-- ============================================================
INSERT INTO safety_facility (facility_type, name, address, location, data_source) VALUES
('POLICE', '분당경찰서', '성남시 분당구 야탑동 556', ST_SetSRID(ST_MakePoint(127.1290, 37.4060), 4326), '경찰청'),
('POLICE', '서현지구대', '성남시 분당구 서현동 280', ST_SetSRID(ST_MakePoint(127.1245, 37.3855), 4326), '경찰청'),
('POLICE', '야탑지구대', '성남시 분당구 야탑동 540', ST_SetSRID(ST_MakePoint(127.1300, 37.4125), 4326), '경찰청'),
('POLICE', '정자지구대', '성남시 분당구 정자동 168', ST_SetSRID(ST_MakePoint(127.1072, 37.3675), 4326), '경찰청'),
('POLICE', '수내치안센터', '성남시 분당구 수내동 55', ST_SetSRID(ST_MakePoint(127.1155, 37.3785), 4326), '경찰청'),
('POLICE', '구미치안센터', '성남시 분당구 구미동 190', ST_SetSRID(ST_MakePoint(127.1098, 37.3560), 4326), '경찰청'),
('POLICE', '판교치안센터', '성남시 분당구 삼평동 695', ST_SetSRID(ST_MakePoint(127.1095, 37.3960), 4326), '경찰청'),
('POLICE', '이매치안센터', '성남시 분당구 이매동 135', ST_SetSRID(ST_MakePoint(127.1260, 37.3965), 4326), '경찰청');

-- ============================================================
-- 5. 위험 구역 (danger_zone) - 격자 기반
--    약 300m x 300m 격자, 안전 점수 0~100 (낮을수록 위험)
--    risk_level: HIGH(0~30), MEDIUM(31~60), LOW(61~100)
-- ============================================================
INSERT INTO danger_zone (grid_polygon, safety_score, facility_count, risk_level) VALUES
-- HIGH 위험: 탄천 산책로 어두운 구간 (서현~수내 사이)
(ST_SetSRID(ST_MakeEnvelope(127.1150, 37.3780, 127.1180, 37.3810), 4326), 22.50, 1, 'HIGH'),
-- HIGH 위험: 불곡산 입구 야간 (가로등 적음)
(ST_SetSRID(ST_MakeEnvelope(127.0990, 37.3700, 127.1020, 37.3730), 4326), 15.00, 0, 'HIGH'),
-- HIGH 위험: 야탑동 이면도로 (외진 주택가)
(ST_SetSRID(ST_MakeEnvelope(127.1310, 37.4120, 127.1340, 37.4150), 4326), 25.00, 1, 'HIGH'),
-- HIGH 위험: 율동공원 야간 (인적 드묾)
(ST_SetSRID(ST_MakeEnvelope(127.0970, 37.3400, 127.1000, 37.3430), 4326), 18.00, 0, 'HIGH'),
-- HIGH 위험: 구미동 외곽 이면도로
(ST_SetSRID(ST_MakeEnvelope(127.1100, 37.3520, 127.1130, 37.3550), 4326), 28.00, 1, 'HIGH'),

-- MEDIUM 위험: 서현동 주택가
(ST_SetSRID(ST_MakeEnvelope(127.1290, 37.3850, 127.1320, 37.3880), 4326), 45.00, 3, 'MEDIUM'),
-- MEDIUM 위험: 정자동 아파트 단지 뒷편
(ST_SetSRID(ST_MakeEnvelope(127.1040, 37.3670, 127.1070, 37.3700), 4326), 52.00, 3, 'MEDIUM'),
-- MEDIUM 위험: 탄천 산책로 (야탑 구간)
(ST_SetSRID(ST_MakeEnvelope(127.1200, 37.4060, 127.1230, 37.4090), 4326), 40.00, 2, 'MEDIUM'),
-- MEDIUM 위험: 이매동 주택가
(ST_SetSRID(ST_MakeEnvelope(127.1270, 37.3960, 127.1300, 37.3990), 4326), 48.00, 2, 'MEDIUM'),
-- MEDIUM 위험: 삼평동 공사지역
(ST_SetSRID(ST_MakeEnvelope(127.1060, 37.3990, 127.1090, 37.4020), 4326), 38.00, 2, 'MEDIUM'),
-- MEDIUM 위험: 탄천 산책로 (정자~미금)
(ST_SetSRID(ST_MakeEnvelope(127.1030, 37.3480, 127.1060, 37.3510), 4326), 42.00, 2, 'MEDIUM'),
-- MEDIUM 위험: 수내동 뒷골목
(ST_SetSRID(ST_MakeEnvelope(127.1130, 37.3750, 127.1160, 37.3780), 4326), 50.00, 3, 'MEDIUM'),

-- LOW 위험: 서현역 상업지구 (밝고 인구 많음)
(ST_SetSRID(ST_MakeEnvelope(127.1240, 37.3820, 127.1270, 37.3850), 4326), 85.00, 12, 'LOW'),
-- LOW 위험: 야탑역 상업지구
(ST_SetSRID(ST_MakeEnvelope(127.1260, 37.4090, 127.1290, 37.4120), 4326), 82.00, 10, 'LOW'),
-- LOW 위험: 정자역 상업지구
(ST_SetSRID(ST_MakeEnvelope(127.1070, 37.3640, 127.1100, 37.3670), 4326), 88.00, 11, 'LOW'),
-- LOW 위험: 수내역 상업지구
(ST_SetSRID(ST_MakeEnvelope(127.1130, 37.3770, 127.1160, 37.3800), 4326), 80.00, 9, 'LOW'),
-- LOW 위험: 미금역 상업지구
(ST_SetSRID(ST_MakeEnvelope(127.1075, 37.3570, 127.1105, 37.3600), 4326), 78.00, 8, 'LOW'),
-- LOW 위험: 판교역 테크노밸리
(ST_SetSRID(ST_MakeEnvelope(127.1090, 37.3930, 127.1120, 37.3960), 4326), 90.00, 13, 'LOW'),
-- LOW 위험: 판교 테크노밸리 중심
(ST_SetSRID(ST_MakeEnvelope(127.1050, 37.4000, 127.1080, 37.4030), 4326), 75.00, 7, 'LOW'),
-- LOW 위험: 오리역 근처
(ST_SetSRID(ST_MakeEnvelope(127.1080, 37.3380, 127.1110, 37.3410), 4326), 72.00, 6, 'LOW');

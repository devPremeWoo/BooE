package org.hyeong.booe.loadtest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadTestSeedRunner implements CommandLineRunner {

    private static final String SEED_PREFIX = "lt_";
    private static final int LESSOR_COUNT = 5_000;
    private static final int LESSEE_COUNT = 5_000;
    private static final int CONTRACT_COUNT = 15_000;
    private static final int TEST_USER_GUARANTEED_CONTRACTS = 5;
    private static final int MONTHLY_RENT_MONTHS = 12;
    private static final int BATCH_CHUNK = 5_000;

    private static final String TEST_MEMBER_CODE = "lt_test_user";
    private static final String TEST_LOGIN_ID = "loadtest_user";
    private static final String TEST_RAW_PASSWORD = "Loadtest1234!";
    private static final String TEST_EMAIL = "loadtest@booe.com";
    private static final String TEST_NAME = "테스트유저";

    private static final LocalDate CONTRACT_BASE_START = LocalDate.of(2026, 1, 1);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (alreadySeeded()) {
            log.info("[Seed] 시드 데이터가 이미 존재합니다. 스킵 (다시 생성하려면 lt_ 데이터 삭제).");
            return;
        }

        log.info("[Seed] 시작 - lessors={}, lessees={}, contracts={}",
                LESSOR_COUNT, LESSEE_COUNT, CONTRACT_COUNT);
        long start = System.currentTimeMillis();

        Long testId = insertTestMember();
        List<Long> lessorIds = insertDummyMembers("lt_lessor_", LESSOR_COUNT);
        List<Long> lesseeIds = insertDummyMembers("lt_lessee_", LESSEE_COUNT);
        insertContracts(testId, lessorIds, lesseeIds);
        insertPaymentSchedules();

        log.info("[Seed] 완료 ({}ms). 테스트 계정 ── loginId={} / password={} / memberId={}",
                System.currentTimeMillis() - start, TEST_LOGIN_ID, TEST_RAW_PASSWORD, testId);
    }

    private boolean alreadySeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE member_code = ?",
                Integer.class, TEST_MEMBER_CODE);
        return count != null && count > 0;
    }

    private void cleanupExistingSeed() {
        String like = SEED_PREFIX + "%";
        jdbcTemplate.update(
                "DELETE s FROM contract_payment_schedule s " +
                        "JOIN contract c ON s.contract_id = c.id " +
                        "JOIN member m ON c.member_id = m.id OR c.lessee_member_id = m.id " +
                        "WHERE m.member_code LIKE ?", like);
        jdbcTemplate.update(
                "DELETE c FROM contract c " +
                        "JOIN member m ON c.member_id = m.id OR c.lessee_member_id = m.id " +
                        "WHERE m.member_code LIKE ?", like);
        jdbcTemplate.update(
                "DELETE FROM member_credential WHERE member_id IN (SELECT id FROM member WHERE member_code LIKE ?)", like);
        jdbcTemplate.update(
                "DELETE FROM member_profile WHERE member_id IN (SELECT id FROM member WHERE member_code LIKE ?)", like);
        jdbcTemplate.update("DELETE FROM member WHERE member_code LIKE ?", like);
    }

    private Long insertTestMember() {
        Timestamp now = now();
        jdbcTemplate.update(
                "INSERT INTO member (role, status, member_code, created_at, updated_at) VALUES (?,?,?,?,?)",
                "MEMBER", "ACTIVE", TEST_MEMBER_CODE, now, now);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM member WHERE member_code = ?", Long.class, TEST_MEMBER_CODE);
        insertTestProfile(id, now);
        insertTestCredential(id);
        return id;
    }

    private void insertTestProfile(Long id, Timestamp now) {
        jdbcTemplate.update(
                "INSERT INTO member_profile (member_id, email, name, phone_number, phone_verified, birth, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)",
                id, TEST_EMAIL, TEST_NAME, "01012345678", true, LocalDate.of(1990, 1, 1), now, now);
    }

    private void insertTestCredential(Long id) {
        jdbcTemplate.update(
                "INSERT INTO member_credential (member_id, login_id, password, password_encoder, credential_Status) VALUES (?,?,?,?,?)",
                id, TEST_LOGIN_ID, passwordEncoder.encode(TEST_RAW_PASSWORD), "BCRYPT", "ACTIVE");
    }

    private List<Long> insertDummyMembers(String codePrefix, int count) {
        Timestamp now = now();
        batchInsertMemberRows(codePrefix, count, now);
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM member WHERE member_code LIKE ? ORDER BY id",
                Long.class, codePrefix + "%");
        batchInsertProfiles(ids, codePrefix, now);
        return ids;
    }

    private void batchInsertMemberRows(String codePrefix, int count, Timestamp now) {
        List<Object[]> rows = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            rows.add(new Object[]{"MEMBER", "ACTIVE", String.format("%s%05d", codePrefix, i), now, now});
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO member (role, status, member_code, created_at, updated_at) VALUES (?,?,?,?,?)",
                rows);
    }

    private void batchInsertProfiles(List<Long> ids, String codePrefix, Timestamp now) {
        List<Object[]> rows = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            rows.add(buildProfileRow(ids.get(i), codePrefix, i, now));
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO member_profile (member_id, email, name, phone_number, phone_verified, birth, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)",
                rows);
    }

    private Object[] buildProfileRow(Long id, String codePrefix, int idx, Timestamp now) {
        String type = codePrefix.replace(SEED_PREFIX, "").replace("_", "");
        return new Object[]{
                id,
                codePrefix + idx + "@booe.com",
                type + idx,
                "010" + String.format("%08d", id % 100_000_000L),
                true,
                LocalDate.of(1990, 1, 1),
                now, now
        };
    }

    private void insertContracts(Long testId, List<Long> lessorIds, List<Long> lesseeIds) {
        List<Object[]> rows = new ArrayList<>(CONTRACT_COUNT);
        appendTestUserContracts(rows, testId, lesseeIds);
        appendDummyContracts(rows, lessorIds, lesseeIds);
        batchInsertContracts(rows);
    }

    private void appendTestUserContracts(List<Object[]> rows, Long testId, List<Long> lesseeIds) {
        for (int i = 0; i < TEST_USER_GUARANTEED_CONTRACTS; i++) {
            rows.add(buildContractRow(testId, randomFrom(lesseeIds), i));
        }
    }

    private void appendDummyContracts(List<Object[]> rows, List<Long> lessorIds, List<Long> lesseeIds) {
        int remaining = CONTRACT_COUNT - TEST_USER_GUARANTEED_CONTRACTS;
        for (int i = 0; i < remaining; i++) {
            rows.add(buildContractRow(randomFrom(lessorIds), randomFrom(lesseeIds), TEST_USER_GUARANTEED_CONTRACTS + i));
        }
    }

    private void batchInsertContracts(List<Object[]> rows) {
        String sql = "INSERT INTO contract " +
                "(member_id, lessee_member_id, address, status, type, " +
                " total_deposit, monthly_rent, start_date, end_date, term_months, created_at, updated_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        for (int from = 0; from < rows.size(); from += BATCH_CHUNK) {
            int to = Math.min(from + BATCH_CHUNK, rows.size());
            jdbcTemplate.batchUpdate(sql, rows.subList(from, to));
            log.info("[Seed] contracts inserted {}/{}", to, rows.size());
        }
    }

    private Object[] buildContractRow(Long lessorId, Long lesseeId, int idx) {
        Timestamp now = now();
        return new Object[]{
                lessorId,
                lesseeId,
                "서울시 강남구 테헤란로 " + ((idx % 999) + 1),
                "SIGNED",
                "MONTHLY",
                10_000_000L,
                500_000L,
                CONTRACT_BASE_START,
                CONTRACT_BASE_START.plusYears(2),
                24,
                now, now
        };
    }

    private void insertPaymentSchedules() {
        List<Long> contractIds = jdbcTemplate.queryForList(
                "SELECT c.id FROM contract c " +
                        "JOIN member m ON c.member_id = m.id OR c.lessee_member_id = m.id " +
                        "WHERE m.member_code LIKE ? GROUP BY c.id",
                Long.class, SEED_PREFIX + "%");

        int totalRows = contractIds.size() * (1 + MONTHLY_RENT_MONTHS);
        List<Object[]> rows = new ArrayList<>(totalRows);
        for (Long contractId : contractIds) {
            appendBalanceSchedule(rows, contractId);
            appendMonthlyRentSchedules(rows, contractId);
        }
        batchInsertSchedules(rows);
    }

    private void appendBalanceSchedule(List<Object[]> rows, Long contractId) {
        rows.add(new Object[]{
                contractId, "BALANCE", 10_000_000L,
                CONTRACT_BASE_START, 1, "PENDING"
        });
    }

    private void appendMonthlyRentSchedules(List<Object[]> rows, Long contractId) {
        for (int month = 1; month <= MONTHLY_RENT_MONTHS; month++) {
            rows.add(new Object[]{
                    contractId, "MONTHLY_RENT", 500_000L,
                    CONTRACT_BASE_START.plusMonths(month), month + 1, "PENDING"
            });
        }
    }

    private void batchInsertSchedules(List<Object[]> rows) {
        String sql = "INSERT INTO contract_payment_schedule " +
                "(contract_id, payment_type, amount, due_date, payment_order, status) " +
                "VALUES (?,?,?,?,?,?)";
        for (int from = 0; from < rows.size(); from += BATCH_CHUNK) {
            int to = Math.min(from + BATCH_CHUNK, rows.size());
            jdbcTemplate.batchUpdate(sql, rows.subList(from, to));
            log.info("[Seed] schedules inserted {}/{}", to, rows.size());
        }
    }

    private Long randomFrom(List<Long> ids) {
        return ids.get(random.nextInt(ids.size()));
    }

    private Timestamp now() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}

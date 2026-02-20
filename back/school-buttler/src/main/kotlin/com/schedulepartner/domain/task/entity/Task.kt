package com.schedulepartner.domain.task.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "tasks",
    indexes = [
        Index(name = "idx_task_user_scheduled", columnList = "user_id, scheduled_at"),
        Index(name = "idx_task_status", columnList = "status"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Task(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(length = 500)
    var description: String? = null,

    /** 예정 시작 시각 */
    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,

    /** 사용자가 입력한 예상 소요 시간 (분) */
    @Column(name = "expected_minutes", nullable = false)
    var expectedMinutes: Int,

    /**
     * 태그별 실제 평균 소요 시간 (분)
     * 시간 왜곡 감지 기능에서 expectedMinutes 와 비교
     * null = 아직 5회 미만 데이터 축적
     */
    @Column(name = "actual_avg_minutes")
    var actualAvgMinutes: Int? = null,

    /**
     * 우선순위 (1 ~ 5)
     * 복구 엔진 점수 계산에 사용: 긴급도×2 + 중요도 + 남은시간역수×3
     */
    @Column(nullable = false)
    var priority: Int = 3,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TaskStatus = TaskStatus.PENDING,

    /** 태그 목록 (쉼표 구분 저장, 조회 시 split) */
    @Column(length = 200)
    var tags: String? = null,

    /** 실제 완료 시각 */
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    /** 복구 엔진에 의해 재배치된 일정 여부 */
    @Column(name = "is_recovered", nullable = false)
    var isRecovered: Boolean = false,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** 태그 문자열 → List 변환 */
    fun getTagList(): List<String> =
        tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    /** List → 태그 문자열 변환 */
    fun setTagList(tagList: List<String>) {
        tags = tagList.joinToString(",")
    }

    /** 일정 완료 처리 */
    fun complete() {
        status = TaskStatus.DONE
        completedAt = LocalDateTime.now()
    }

    /** 실제 소요 시간을 반영해 평균 업데이트 (이동 평균) */
    fun updateActualMinutes(actualMinutes: Int, count: Int) {
        actualAvgMinutes = if (actualAvgMinutes == null) {
            actualMinutes
        } else {
            ((actualAvgMinutes!! * (count - 1)) + actualMinutes) / count
        }
    }

    /**
     * 시간 왜곡 감지: 예상 vs 실제 평균 차이가 30% 이상이면 true
     * 기획서: "동일 태그 5회 이상 데이터 ±30% 차이"
     */
    fun isTimeDistorted(): Boolean {
        val avg = actualAvgMinutes ?: return false
        val diff = Math.abs(avg - expectedMinutes).toDouble() / expectedMinutes
        return diff >= 0.3
    }
}
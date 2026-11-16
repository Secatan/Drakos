package com.example.data

import androidx.room.TypeConverter
import com.example.domain.model.DifferenceStatus
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.MovementType
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ShiftStatus
import com.example.domain.model.TransferStatus
import com.example.domain.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (e: Exception) {
        UserRole.EMPLOYEE
    }

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = try {
        MovementType.valueOf(value)
    } catch (e: Exception) {
        MovementType.SALE
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromTransferStatus(value: TransferStatus): String = value.name

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus = try {
        TransferStatus.valueOf(value)
    } catch (e: Exception) {
        TransferStatus.REQUESTED
    }

    @TypeConverter
    fun fromShiftStatus(value: ShiftStatus): String = value.name

    @TypeConverter
    fun toShiftStatus(value: String): ShiftStatus = try {
        ShiftStatus.valueOf(value)
    } catch (e: Exception) {
        ShiftStatus.OPEN
    }

    @TypeConverter
    fun fromDifferenceStatus(value: DifferenceStatus): String = value.name

    @TypeConverter
    fun toDifferenceStatus(value: String): DifferenceStatus = try {
        DifferenceStatus.valueOf(value)
    } catch (e: Exception) {
        DifferenceStatus.NORMAL
    }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = try {
        ExpenseCategory.valueOf(value)
    } catch (e: Exception) {
        ExpenseCategory.VARIOS
    }
}

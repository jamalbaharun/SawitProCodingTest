package com.jamalullail.sawitprotest.domain.mapper

import com.jamalullail.sawitprotest.data.local.TicketEntity
import com.jamalullail.sawitprotest.domain.model.Ticket
import org.junit.Assert.assertEquals
import org.junit.Test

class TicketMapperTest {

    @Test
    fun `toDomain maps TicketEntity to Domain Ticket correctly`() {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val entity = TicketEntity(
            id = "uuid-123",
            dateTime = timestamp,
            licenseNumber = "B 1234 ABC",
            driverName = "John Doe",
            inboundWeight = 1000.0,
            outboundWeight = 800.0,
            netWeight = 200.0,
            isSynced = true
        )

        // Act
        val domain = entity.toDomain()

        // Assert
        assertEquals(entity.id, domain.id)
        assertEquals(entity.dateTime, domain.dateTime)
        assertEquals(entity.licenseNumber, domain.licenseNumber)
        assertEquals(entity.driverName, domain.driverName)
        assertEquals(entity.inboundWeight, domain.inboundWeight, 0.0)
        assertEquals(entity.outboundWeight, domain.outboundWeight, 0.0)
        assertEquals(entity.netWeight, domain.netWeight, 0.0)
        assertEquals(entity.isSynced, domain.isSynced)
    }

    @Test
    fun `toEntity maps Domain Ticket to TicketEntity correctly`() {
        // Arrange
        val timestamp = System.currentTimeMillis()
        val domain = Ticket(
            id = "uuid-123",
            dateTime = timestamp,
            licenseNumber = "B 1234 ABC",
            driverName = "John Doe",
            inboundWeight = 1000.0,
            outboundWeight = 800.0,
            netWeight = 200.0,
            isSynced = false
        )

        // Act
        val entity = domain.toEntity()

        // Assert
        assertEquals(domain.id, entity.id)
        assertEquals(domain.dateTime, entity.dateTime)
        assertEquals(domain.licenseNumber, entity.licenseNumber)
        assertEquals(domain.driverName, entity.driverName)
        assertEquals(domain.inboundWeight, entity.inboundWeight, 0.0)
        assertEquals(domain.outboundWeight, entity.outboundWeight, 0.0)
        assertEquals(domain.netWeight, entity.netWeight, 0.0)
        assertEquals(domain.isSynced, entity.isSynced)
    }
}

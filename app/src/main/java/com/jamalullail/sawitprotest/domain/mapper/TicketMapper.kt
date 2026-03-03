package com.jamalullail.sawitprotest.domain.mapper

import com.jamalullail.sawitprotest.data.local.TicketEntity
import com.jamalullail.sawitprotest.domain.model.Ticket

fun TicketEntity.toDomain(): Ticket {
    return Ticket(
        id = id,
        dateTime = dateTime,
        licenseNumber = licenseNumber,
        driverName = driverName,
        inboundWeight = inboundWeight,
        outboundWeight = outboundWeight,
        netWeight = netWeight,
        isSynced = isSynced
    )
}

fun Ticket.toEntity(): TicketEntity {
    return TicketEntity(
        id = id,
        dateTime = dateTime,
        licenseNumber = licenseNumber,
        driverName = driverName,
        inboundWeight = inboundWeight,
        outboundWeight = outboundWeight,
        netWeight = netWeight,
        isSynced = isSynced
    )
}

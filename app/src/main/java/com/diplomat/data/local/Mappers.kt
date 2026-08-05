package com.diplomat.data.local

import com.diplomat.domain.model.InterceptedMessage

fun MessageEntity.toDomain(): InterceptedMessage = InterceptedMessage(
    id = id,
    sender = sender,
    body = body,
    packageName = packageName,
    timestamp = timestamp,
    status = status,
    toneAnalysis = toneAnalysis,
    requiresResponse = requiresResponse,
    draftResponse = draftResponse,
    userAgreement = userAgreement,
    userReasoning = userReasoning,
)

fun InterceptedMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    sender = sender,
    body = body,
    packageName = packageName,
    timestamp = timestamp,
    status = status,
    toneAnalysis = toneAnalysis,
    requiresResponse = requiresResponse,
    draftResponse = draftResponse,
    userAgreement = userAgreement,
    userReasoning = userReasoning,
)

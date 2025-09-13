package com.ivelosi.dnc.domain.model.device

import com.ivelosi.dnc.data.local.account.AccountEntity
import com.ivelosi.dnc.network.model.device.NetworkAccount

data class Account(
    val accountId: Long,
    val profileUpdateTimestamp: Long
)

fun Account.toAccountEntity(): AccountEntity {
    return AccountEntity(
        accountId = accountId,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun AccountEntity.toAccount(): Account {
    return Account(
        accountId = accountId,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun Account.toNetworkAccount(): NetworkAccount {
    return NetworkAccount(
        accountId = accountId,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun NetworkAccount.toAccount(): Account {
    return Account(
        accountId = accountId,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}
package com.ivelosi.dnc.domain.model.device

import com.ivelosi.dnc.data.local.account.AccountEntity
import com.ivelosi.dnc.network.model.device.NetworkAccount

data class Account(
    val Nid: Long,
    val profileUpdateTimestamp: Long
)

fun Account.toAccountEntity(): AccountEntity {
    return AccountEntity(
        Nid = Nid,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun AccountEntity.toAccount(): Account {
    return Account(
        Nid = Nid,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun Account.toNetworkAccount(): NetworkAccount {
    return NetworkAccount(
        Nid = Nid,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}

fun NetworkAccount.toAccount(): Account {
    return Account(
        Nid = Nid,
        profileUpdateTimestamp = profileUpdateTimestamp
    )
}
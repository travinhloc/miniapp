package com.vault.vanishx.domain.model

/** Sanitize Base64 pubkeys for Firebase path segments. */
fun firebaseSafeKey(raw: String): String =
    raw.replace('+', '-').replace('/', '_').replace("=", "")

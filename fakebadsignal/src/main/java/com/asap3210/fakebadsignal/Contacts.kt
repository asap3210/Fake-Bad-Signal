package com.asap3210.fakebadsignal

import android.content.Context
import android.provider.ContactsContract

data class Contact(val name: String, val number: String, val key: String)

object Contacts {
    fun load(ctx: Context): List<Contact> {
        val out = LinkedHashMap<String, Contact>()
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(0) ?: continue
                val num = c.getString(1) ?: continue
                val key = Prefs.key(num)
                if (key.isNotEmpty() && key !in out) out[key] = Contact(name, num, key)
            }
        }
        return out.values.toList()
    }
}

package br.com.leorvergani.escalaici.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirebasePlatformFactoryTest {
    @Test
    fun corporateScheduleFactoryKeepsCorporateProjectId() {
        val gateway = assertIs<FirestoreRestGateway>(createFirebaseScheduleGateway())

        assertEquals("escalaici", gateway.projectId)
    }

    @Test
    fun demoPublicationFactoryUsesFirebaseDevProjectIdFromLocalConfig() {
        val gateway = assertIs<FirestoreRestGateway>(createDemoPublicationGateway())

        assertEquals("escala-ici-dev", gateway.projectId)
    }
}

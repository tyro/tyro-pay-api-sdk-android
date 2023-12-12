package com.tyro.payapi.retrofit

import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RetrofitFactoryTest {
    private lateinit var factory: RetrofitFactory

    @Before
    fun setUp() {
        factory = RetrofitFactory()
    }

    @Test
    fun `createRetrofit() should return retrofit instance with correct params`() {
        val retrofit = factory.createRetrofit("https://some.url")
        Assert.assertNotNull(retrofit)
        assertEquals("https://some.url/", retrofit.baseUrl().toString())
        assertEquals(retrofit2.converter.gson.GsonConverterFactory::class.java, retrofit.converterFactories()[1].javaClass)
    }
}

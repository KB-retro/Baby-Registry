package com.kbretro.babyregistry.domain

import com.kbretro.babyregistry.data.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GrowthTest {
    private val weightBoys = WhoGrowth.table(GrowthIndicator.WEIGHT, Sex.BOY)!!

    @Test
    fun `la mediana al nacer coincide con la tabla de la OMS`() {
        assertEquals(3.3464, WhoGrowth.valueAt(weightBoys, 0.0, 0.0)!!, 1e-4)
        assertEquals(0.0, WhoGrowth.zScore(weightBoys, 0.0, 3.3464)!!, 1e-6)
        assertEquals(50.0, WhoGrowth.percentile(0.0), 1e-6)
    }

    @Test
    fun `los valores a +2 DE coinciden con las tablas publicadas`() {
        // Tabla OMS peso/edad niños: 0 meses +2 DE = 4,4 kg; 12 meses +2 DE = 12,0 kg.
        assertEquals(4.4, WhoGrowth.valueAt(weightBoys, 0.0, 2.0)!!, 0.05)
        assertEquals(12.0, WhoGrowth.valueAt(weightBoys, 12.0, 2.0)!!, 0.05)
        // Talla/edad niñas a los 24 meses, mediana 86,4 cm.
        val lengthGirls = WhoGrowth.table(GrowthIndicator.LENGTH, Sex.GIRL)!!
        assertEquals(86.4, WhoGrowth.valueAt(lengthGirls, 24.0, 0.0)!!, 0.05)
    }

    @Test
    fun `percentiles de referencia`() {
        assertEquals(97.0, WhoGrowth.percentile(1.8808), 0.05)
        assertEquals(3.0, WhoGrowth.percentile(-1.8808), 0.05)
        assertEquals(84.13, WhoGrowth.percentile(1.0), 0.01)
    }

    @Test
    fun `sin sexo indicado o fuera de rango no hay curva`() {
        assertNull(WhoGrowth.table(GrowthIndicator.WEIGHT, Sex.UNSPECIFIED))
        assertNull(WhoGrowth.valueAt(weightBoys, 61.0, 0.0))
    }
}

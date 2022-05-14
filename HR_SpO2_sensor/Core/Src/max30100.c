#include "max30100.h"

static I2C_HandleTypeDef *i2c;
static USART_HandleTypeDef *usart;
static uint16_t ir_buffer[512];
static uint16_t red_buffer[512];
static uint16_t ring_buffer_write_index;
static uint16_t ring_buffer_read_index;

void MAX30100_Init(I2C_HandleTypeDef *hi2c, USART_HandleTypeDef *husart)
{
    i2c = hi2c;
    usart = husart;

    HAL_StatusTypeDef status;
    uint8_t val = 0x00;

    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, FIFO_WRITE_POINTER_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, OVER_FLOW_COUNTER_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, FIFO_READ_POINTER_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    uint8_t it_status = 0;
    status = HAL_I2C_Mem_Read(i2c, MAX30100_ADDR, INTERRUPT_STATUS, 1, &it_status, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);
}

void MAX30100_Default_Config(void)
{
    HAL_StatusTypeDef status;
    uint8_t val;

    val = INTERRUPT_ENABLE_VAL;
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, INTERRUPT_ENABLE_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    val = MODE_CONFIGURATION_VAL;
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, MODE_CONFIGURATION_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    val = SPO2_CONFIGURATION_VAL;
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, SPO2_CONFIGURATION_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    val = LED_CONFIGURATION_VAL;
    status = HAL_I2C_Mem_Write(i2c, MAX30100_ADDR, LED_CONFIGURATION_REG, 1, &val, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);
}

bool MAX30100_Data_Get(uint16_t *ir, uint16_t *red)
{
    if (ring_buffer_write_index != ring_buffer_read_index)
    {
        *ir = ir_buffer[ring_buffer_read_index];
        *red = red_buffer[ring_buffer_read_index];
        ring_buffer_read_index++;
        ring_buffer_read_index %= 512;
        return true;
    }
    return false;
}

static void FIFO_Read(void)
{
    uint8_t ir_high = 0;
    uint8_t ir_low = 0;
    uint8_t red_high = 0;
    uint8_t red_low = 0;
    uint8_t data_buf[64];
    HAL_StatusTypeDef status;
    uint8_t samples_num = 15;

    status = HAL_I2C_Mem_Read(i2c, MAX30100_ADDR, FIFO_DATA_REG, 1, data_buf, samples_num * 4, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    for (uint16_t i = 0; i < samples_num; i++)
    {
        ir_high = data_buf[4*i + 0];
        ir_low = data_buf[4*i + 1];
        red_high = data_buf[4*i + 2];
        red_low = data_buf[4*i + 3];
        ir_buffer[ring_buffer_write_index] = ((uint16_t)ir_high << 8) | (ir_low);
        red_buffer[ring_buffer_write_index] = ((uint16_t)red_high << 8) | (red_low);

        ring_buffer_write_index++;
        ring_buffer_write_index %= 512;
    }
}

void MAX30100_Interrupt_Handler(void)
{
    HAL_StatusTypeDef status;
    uint8_t it_status = 0;
    status = HAL_I2C_Mem_Read(i2c, MAX30100_ADDR, INTERRUPT_STATUS, 1, &it_status, 1, HAL_MAX_DELAY);
    HAL_ASSERT(status);

    if((it_status >> INTERRUPT_STATUS_FIFO_FULL) & 0x01)
    {
        FIFO_Read();
    }
}


#include "max30100.h"

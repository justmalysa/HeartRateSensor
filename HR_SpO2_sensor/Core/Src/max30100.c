#include "max30100.h"
#include <string.h>
#include <math.h>

#define SAMPLING_TIME_MS      10
#define WINDOW_SIZE           32
#define DATA_WINDOW_SIZE      16
#define HR_VALUES_CNT         256
#define SPO2_VALUES_CNT       2048
#define LED_BUFFER_SIZE       512
#define MEASUREMENT_PERIOD    15

static I2C_HandleTypeDef *i2c;
static UART_HandleTypeDef *uart;

static uint16_t ir_buffer[LED_BUFFER_SIZE];
static uint16_t red_buffer[LED_BUFFER_SIZE];

static uint16_t ringbuf_write_index;
static uint16_t ringbuf_read_index;
static uint16_t ringbuf_val[WINDOW_SIZE];
static uint16_t hr_buffer[HR_VALUES_CNT];
static uint16_t hr_val;
static uint16_t spo2_val;

static uint16_t hr_index;
static uint16_t ringbuf_val_index;
static uint16_t spo2_index;

static uint16_t max_val = 0;
static uint16_t max_index;
static uint16_t min_val = UINT16_MAX;
static uint16_t min_index;
static uint16_t last_max_index;
static uint16_t moving_average_sum;
static uint64_t ir_sum;
static uint64_t red_sum;


static void Variables_Init(void)
{
    memset(hr_buffer, 0, sizeof(hr_buffer));
    memset(ringbuf_val, 0, sizeof(ringbuf_val));

    hr_index = 0;
    ringbuf_val_index = 0;
    spo2_index = 0;
    max_val = 0;
    max_index = 0;
    min_val = UINT16_MAX;
    min_index = 0;
    last_max_index = 0;
    moving_average_sum = 0;
    ir_sum = 0;
    red_sum = 0;
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
        ir_buffer[ringbuf_write_index] = ((uint16_t)ir_high << 8) | (ir_low);
        red_buffer[ringbuf_write_index] = ((uint16_t)red_high << 8) | (red_low);

        ringbuf_write_index++;
        ringbuf_write_index %= 512;
    }
}

static void HR_Sample_Add(uint16_t val)
{
    uint16_t abs_index = ringbuf_val_index;
    ringbuf_val_index += 1;

    uint16_t next_index = abs_index % WINDOW_SIZE;

    moving_average_sum -= ringbuf_val[next_index];
    moving_average_sum += val;

    ringbuf_val[next_index] = val;

    if (abs_index < WINDOW_SIZE)
    {
        return;
    }

    uint16_t moving_average = moving_average_sum / WINDOW_SIZE;

    if (moving_average >= max_val)
    {
        max_val = moving_average;
        max_index = abs_index;
    }

    if (moving_average <= min_val)
    {
        min_val = moving_average;
        min_index = abs_index;
    }

    uint16_t middle_index = abs_index - (DATA_WINDOW_SIZE / 2);
    if (max_index == middle_index)
    {
        if (last_max_index > 0)
        {
            hr_buffer[hr_index++] = 60000 / ((max_index - last_max_index) * SAMPLING_TIME_MS);
        }
        last_max_index = max_index;
        min_val = UINT16_MAX;
    }

    if (min_index == middle_index)
    {
        max_val = 0;
    }
}

static void SpO2_Sample_Add(uint16_t ir_val, uint16_t red_val)
{
    ir_sum += (uint32_t)ir_val * (uint32_t)ir_val;
    red_sum += (uint32_t)red_val * (uint32_t)red_val;
    spo2_index++;
}

static void HR_Buffer_Sort(void)
{
    for (uint16_t i = 1; i < (hr_index - 1); i++)
    {
        uint16_t hr_tmp = 0;
        for (uint16_t j = 1; j < (hr_index - i - 1); j++)
        {
            if (hr_buffer[j] > hr_buffer[j + 1])
            {
                hr_tmp = hr_buffer[j + 1];
                hr_buffer[j + 1] = hr_buffer[j];
                hr_buffer[j] = hr_tmp;
            }
        }
    }
}

void MAX30100_Init(I2C_HandleTypeDef *hi2c, UART_HandleTypeDef *huart)
{
    i2c = hi2c;
    uart = huart;

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
    if (ringbuf_write_index != ringbuf_read_index)
    {
        *ir = ir_buffer[ringbuf_read_index];
        *red = red_buffer[ringbuf_read_index];
        ringbuf_read_index++;
        ringbuf_read_index %= 512;
        return true;
    }
    return false;
}

void MAX30100_Sample_Add(uint16_t ir_val, uint16_t red_val)
{
    SpO2_Sample_Add(ir_val, red_val);
    HR_Sample_Add(ir_val);
}

void MAX30100_Measurement_Complete(void)
{
    HR_Buffer_Sort();
    hr_val = hr_buffer[hr_index / 2];

    float AC_red = sqrt((float)red_sum / MEASUREMENT_PERIOD);
    float DC_red = (float)red_sum / spo2_index;
    float AC_ir = sqrt((float)ir_sum / MEASUREMENT_PERIOD);
    float DC_ir = (float)ir_sum / spo2_index;
    spo2_val = 110.0 - 25.0 * ((AC_red / DC_red) / (AC_ir / DC_ir));

    Variables_Init();
}

void MAX30100_HR_SpO2_Send(void)
{
    HAL_StatusTypeDef status;
    char buffer[256];
    size_t bytes = 0;
    bytes = 0;
    bytes += snprintf(&buffer[bytes], sizeof(buffer) - bytes, "HR: %u\nSpO2: %u\n", hr_val, spo2_val);
    status = HAL_UART_Transmit(uart, (uint8_t *)buffer, bytes, HAL_MAX_DELAY);
    HAL_ASSERT(status);
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

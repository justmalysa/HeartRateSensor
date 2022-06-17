#ifndef INC_MAX30100_H_
#define INC_MAX30100_H_

#include "main.h"
#include "stdbool.h"
#include <stdio.h>

#define INTERRUPT_STATUS              0x00
#define INTERRUPT_ENABLE_REG          0x01
#define FIFO_WRITE_POINTER_REG        0x02
#define OVER_FLOW_COUNTER_REG         0x03
#define FIFO_READ_POINTER_REG         0x04
#define FIFO_DATA_REG                 0x05
#define MODE_CONFIGURATION_REG        0x06
#define SPO2_CONFIGURATION_REG        0x07
#define LED_CONFIGURATION_REG         0x09

#define PWR_RDY                       0x01

#define MAX30100_ADDR                 0xAE
#define MAX30100_WRITE_ADDR           0xAE
#define MAX30100_READ_ADDR            0xAF

#define INTERRUPT_FIFO_FULL_EN        (0x01 << 7)
#define INTERRUPT_STATUS_FIFO_FULL    0x07

#define MODE_HR_ONLY_EN               0x02
#define MODE_SPO2_EN                  0x03
#define MODE_RESET                    (0x01 << 6)

#define SPO2_HI_RES_EN                (0x01 << 6)
#define SPO2_SR_100_PER_S             (0x01 << 2)
#define LED_PW_1600_US                0x03

#define RED_LED_CURRENT_50_MA         (0x0F << 4)
#define IR_LED_CURRENT_20_8_MA        0x06

#define INTERRUPT_ENABLE_VAL          INTERRUPT_FIFO_FULL_EN
#define MODE_CONFIGURATION_VAL        MODE_SPO2_EN
#define SPO2_CONFIGURATION_VAL        (SPO2_HI_RES_EN | SPO2_SR_100_PER_S | LED_PW_1600_US)
#define LED_CONFIGURATION_VAL         (RED_LED_CURRENT_50_MA | IR_LED_CURRENT_20_8_MA)


/**
 * @brief Function for initializing sensor.
 *
 * @param hi2c  Pointer to I2C instance.
 * @param huart Pointer to USART instance.
 */
void MAX30100_Init(I2C_HandleTypeDef *hi2c, UART_HandleTypeDef *huart);


/** @brief Function for setting default configuration of sensor. */
void MAX30100_Default_Config(void);

/**
 * @brief Function for getting IR and RED raw date from ring buffer.
 *
 * @param ir  Pointer to IR raw value.
 * @param red Pointer to RED raw value.
 *
 * @return true if getting was successful, false otherwise
 */
bool MAX30100_Data_Get(uint16_t *ir, uint16_t *red);

/**
 * @brief Function for adding raw data to the algorithms.
 *
 * @param ir_val  Raw data of IR light.
 * @param red_val Raw data of RED light.
 */
void MAX30100_Sample_Add(uint16_t ir_val, uint16_t red_val);

/** @brief Function for completing global HR and SpO2 values after measurement. */
void MAX30100_Measurement_Complete(void);

/** @brief Function for sending HR and SpO2 values via USART. */
void MAX30100_HR_SpO2_Send(void);

/** @brief Function to handle the interrupt. */
void MAX30100_Interrupt_Handler(void);

#endif /* INC_MAX30100_H_ */

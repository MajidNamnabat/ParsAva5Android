#ifndef SAMPLERATECOMMON
#define SAMPLERATECOMMON

#pragma once
/*
** User supplied callback function type for use with src_callback_new()
** and src_callback_read(). First parameter is the same pointer that was
** passed into src_callback_new(). Second parameter is pointer to a
** pointer. The user supplied callback function must modify *data to
** point to the start of the user supplied float array. The user supplied
** function must return the number of frames that **data points to.
*/

typedef long (*src_callback_t) (void *cb_data, float **data) ;

enum SRC_MODE
{
    SRC_MODE_PROCESS	= 0,
    SRC_MODE_CALLBACK	= 1
} ;

typedef enum SRC_ERROR
{
    SRC_ERR_NO_ERROR = 0,

    SRC_ERR_MALLOC_FAILED,
    SRC_ERR_BAD_STATE,
    SRC_ERR_BAD_DATA,
    SRC_ERR_BAD_DATA_PTR,
    SRC_ERR_NO_PRIVATE,
    SRC_ERR_BAD_SRC_RATIO,
    SRC_ERR_BAD_PROC_PTR,
    SRC_ERR_SHIFT_BITS,
    SRC_ERR_FILTER_LEN,
    SRC_ERR_BAD_CONVERTER,
    SRC_ERR_BAD_CHANNEL_COUNT,
    SRC_ERR_SINC_BAD_BUFFER_LEN,
    SRC_ERR_SIZE_INCOMPATIBILITY,
    SRC_ERR_BAD_PRIV_PTR,
    SRC_ERR_BAD_SINC_STATE,
    SRC_ERR_DATA_OVERLAP,
    SRC_ERR_BAD_CALLBACK,
    SRC_ERR_BAD_MODE,
    SRC_ERR_NULL_CALLBACK,
    SRC_ERR_NO_VARIABLE_RATIO,
    SRC_ERR_SINC_PREPARE_DATA_BAD_LEN,
    SRC_ERR_BAD_INTERNAL_STATE,

    /* This must be the last error number. */
    SRC_ERR_MAX_ERROR
} SRC_ERROR ;

typedef struct SRC_STATE_tag SRC_STATE ;

/* SRC_DATA is used to pass data to src_simple() and src_process(). */
typedef struct
{	const float	*data_in ;
    float	*data_out ;

    long	input_frames, output_frames ;
    long	input_frames_used, output_frames_gen ;

    int		end_of_input ;

    double	src_ratio ;
} SRC_DATA ;

typedef struct SRC_STATE_VT_tag
{
    /* Varispeed process function. */
    SRC_ERROR		(*vari_process) (SRC_STATE *state, SRC_DATA *data) ;

    /* Constant speed process function. */
    SRC_ERROR		(*const_process) (SRC_STATE *state, SRC_DATA *data) ;

    /* State reset. */
    void			(*reset) (SRC_STATE *state) ;

    /* State clone. */
    SRC_STATE		*(*copy) (SRC_STATE *state) ;

    /* State close. */
    void			(*close) (SRC_STATE *state) ;
} SRC_STATE_VT ;

struct SRC_STATE_tag
{
    SRC_STATE_VT *vt ;

    double	last_ratio, last_position ;

    SRC_ERROR	error ;
    int		channels ;

    /* SRC_MODE_PROCESS or SRC_MODE_CALLBACK */
    enum SRC_MODE	mode ;

    /* Data specific to SRC_MODE_CALLBACK. */
    src_callback_t	callback_func ;
    void			*user_callback_data ;
    long			saved_frames ;
    const float		*saved_data ;

    /* Pointer to data to converter specific data. */
    void	*private_data ;
} ;
/*
** The following enums can be used to set the interpolator type
** using the function src_set_converter().
*/

enum
{
    SRC_SINC_BEST_QUALITY		= 0,
    SRC_SINC_MEDIUM_QUALITY		= 1,
    SRC_SINC_FASTEST			= 2,
    SRC_ZERO_ORDER_HOLD			= 3,
    SRC_LINEAR					= 4,
} ;

#endif
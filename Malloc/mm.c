/*
 *******************************************************************************
 *                               mm.c                                					 *         
 *           64-bit struct-based explicit free list memory allocator           *         
 *           							Name: Jiupeng Sun																		 * 				
 *           							Andrew ID: jiupengs																	 * 				
 *           							email: jiupengs@andrew.cmu.edu											 * 				
 *                                                                             *         
 *  ************************************************************************   *         
 *                               DOCUMENTATION                                 *         
 *                                                                             *         
 *  ** STRUCTURE. **                                                           *         
 *                                                                             *         
 *  Both allocated and free blocks share the same struct. While blocks         *         
 *  are processed seperately according to its size--size larger than           *         
 *  16 bytes has same structure, while equal to is has another special         *         
 *  stucture, we'll talk about it below:                                       *         
 *  ------------------------------32 bytes structure----------------------     *         
 *  BLOCK WITH SIZE >= 32 BYTES:                                               *         
 *  HEADER: 8-byte, aligned to 8th byte of an 16-byte aligned heap, where      *         
 *          - The lowest order bit is 1 when the block is allocated, and       *         
 *            0 otherwise. The second lowest bit represents the allocation		 *         
 *            status of former block. 1 means allocation, otherwise 0.				 *         
 *            The 3rd lowest bit is set if this block is a tiny block(16-byte),*					
 *            0 otherwise.																										 * 				
 *  FOOTER: 8-byte, aligned to 0th byte of an 16-byte aligned heap. It         *         
 *          contains the exact copy of the block's header.                     *         
 *                                                                             *         
 *  Allocated blocks contain the following:                                    *         
 *  HEADER, as defined above.                                                  *         
 *  PAYLOAD: Memory allocated for program to store information.                *         
 *  The size of an allocated block is exactly PAYLOAD + HEADER.      					 *         
 *                                                                             *         
 *  Free blocks contain the following:                                         *         
 *  HEADER, as defined above.                                                  *         
 *  PREV_POINTER, pointer to the prev free block															 *         
 *  NEXT_POINTER, pointer to the next free block															 *         
 *  PAYLOAD: if size larger than 32 bytes has this field											 * 				
 *  FOOTER, as defined above.                                                  *         
 *  The size of an unallocated block is at least 32 bytes.                     *         
 *                                                                             *         
 *  Block Visualization.                                                       *         
 *                    block     block+8          					 block+size      		 *         
 *  Allocated blocks:   | HEADER |  .......... PAYLOAD .......... |            *         
 *                                                                             *         
 *                    block block+8 block+16 block+32	block+size-8 block+size	 *         
 *  Unallocated blocks: | HEADER | PREV | NEXT | (empty) | FOOTER |            *         
 *                                                                             *         
 *                                                                             *         
 *  ------------------------------16 bytes structure----------------------     *         
 *  BLOCK WITH SIZE = 16 BYTES                                                 *         
 *  Allocated blocks contain the following:                                    *         
 *  HEADER: 8-byte, same with the header of 32-byte's header.                  *         
 *  PAYLOAD: 8-byte.                                                           *         
 *                                                                             *         
 *  Free blocks contain the following:                                         *         
 *  PREV POINTER: 8-byte, contains a pointer to its prev 16-byte block         *         
 *  		3 flag bits in the lowest position, same with the header of            *         
 *  		32-byte block.                                                         *         
 *  NEXT POINTER: 8-byte, contains a pointer to its next 16-byte block         *         
 *  		3 flag bits in the lowest position.                                    *         
 *                                                                             *         
 *  Block Visualization:                                                       *         
 *									block		block+8		block+16                                 *         
 *  Allocated blocks: | HEADER | PAYLOAD |                                     *         
 *		                                                                         *         
 *										block					block+8				block+16                     *         
 *  Unallocated blocks: | PREV POINTER | NEXT POINTER |                        *         
 *                                                                             *         
 *  ************************************************************************   *         
 *  ** INITIALIZATION. **                                                      *         
 *                                                                             *         
 *  The following visualization reflects the beginning of the heap.            *         
 *      start            	start+buffer_size      start+bs+8        *           *
 *  INIT: | SEGRAGATED LIST BUFFER | PROLOGUE_FOOTER | FIRST CHUNK             *
 *		start+bs+chunksize+16                                                    *
 *  			| EPILOGUE_HEADER |  *                                               *
 *  SEGRAGATED LIST: a 16-byte alignment buffer which contains pointers			   *         
 *  								 to free block list. Different index of array point				 *         
 *  								 to a different size of block.                             *         
 *  PROLOGUE_FOOTER: 8-byte footer, as defined above, that simulates the       *         
 *                    end of an allocated block. Also serves as padding.       *         
 *  FIRST CHUNK: first free chunk that could use.                              *         
 *  EPILOGUE_HEADER: 8-byte block indicating the end of the heap.              *         
 *                   It simulates the beginning of an allocated block          *         
 *                   The epilogue header is moved when the heap is extended.   *         
 *                                                                             *         
 *  ************************************************************************   *         
 *  ** BLOCK ALLOCATION. **                                                    *         
 *                                                                             *         
 *  After initialization, there should be at least a free block with size 		 *         
 *  /chunksize/ bytes free block in the heap, it also will be aligned as last  *         
 *  split block.                                                               *         
 *  Each time malloc request will excute following steps:                      *         
 *  1. align the request size to 16-byte alignment;                            *         
 *  2. If request size is 16 bytes, check the fast bin, which contains only    *         
 *  16-byte valid block. Blocks in the fast bin are the blocks freed before,   *         
 *  but not set the alloc bit to free-indicated yet. Doing so is to cache the  *         
 *  16-byte blocks, if there are other request in the future that demands      *         
 *  16-byte block, we could process it immediately;                            *         
 *  3. If the size is small or equal than MIN_SMALL_BLOCK_SIZE, search it in   *         
 *  the small bins, using LIFO searching, each small bin only contains a       *
 *  specific size block;                                                       *         
 *  4. If size is larger than MIN_SMALL_BLOCK_SIZE, or we didn't find a exactly*         
 *  matched small block in the previous step, then using best-fit searching    *
 *  here, to find a block that just could place the request size;              *         
 *	5. If there isn't suitable block, then we need to allocate more space      *         
 *	from the system. Here we allocate max(chunsize, demand_size) bytes         *         
 *	space. chunsize is a 16-multiple bytes size, and demand_size is            *         
 *	allocating size - (payload size of last block) when and only when last     *         
 *	block is free. After allocate new space, we need to excute a coalesce      *         
 *	and extend heap by setting new epilogue block. And then we put it into     *         
 *	free block list.                                                           *         
 *	6. After finding a suitable space, I use /place/ function to do some       *
 *	necessary	steps to tag this block as allocated block;                      *                   
 *	7. Then return the pointer to application.                                 *         
 *                                                                             *         
 *	Each time free request will excute following steps:                        *         
 *	1. If size is equal to 16 bytes, then add it into fast bin. If fast bin    *
 *	has been full(reach the MAX_FREE_BLOCK_BUFFER_SIZE), then remove and add   *
 *	the first-in block into free-block list;                                   *                            
 *	2. Else free this block immediately, by set the flag bits, and coalesce    *
 *	it with	previous and next free block, if exists, and then insert it into   *
 *	free-block list	using different insert alogorithms.                        *                         
 *                                                                             *         
 *	I refered realloc and calloc function from mm-baseline.c. I change 				 *         
 *	nothing with them since this two function only need simple implementation	 *         
 *                                                                             *         
 *  ************************************************************************   *         
 *  ** NOTICE **                                                							 *         
 * 	Each time update header, the allocated status of next block should be      *         
 * 	considerated.																															 *         
 * 	mm_checkheap would check each block in the heap including free block       *         
 * 	array, prologue and epilogue, each payload block.                          *         
 *                                                                             *         
 *******************************************************************************
 */

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

#include "mm.h"
#include "memlib.h"

/*
 * If you want debugging output, uncomment the following.  Be sure not
 * to have debugging enabled in your final submission
 */
//#define DEBUG
#define ASSERT

#ifdef DEBUG
/* When debugging is enabled, the underlying functions get called */
#define dbg_printf(...) printf(__VA_ARGS__)
#define dbg_checkheap(...) mm_checkheap(__VA_ARGS__)
#define dbg_printheap(...) print_heap(__VA_ARGS__)
#define dbg_printBlock(...) printBlock(__VA_ARGS__)
#define dbg_printHeap(...) printHeap(__VA_ARGS__)
#else
/* When debugging is disabled, no code gets generated */
#define dbg_printf(...)
#define dbg_checkheap(...)
#define dbg_printheap(...)
#define dbg_printBlock(...) 
#define dbg_printHeap(...) 
#endif

#ifdef ASSERT
/* When assert is enabled, the underlying functions get called */
#define dbg_assert(...) assert(__VA_ARGS__)
#define dbg_requires(...) assert(__VA_ARGS__)
#define dbg_ensures(...) assert(__VA_ARGS__) 
#else
/* When asserting is disabled, no code gets generated */
#define dbg_assert(...)
#define dbg_requires(...)
#define dbg_ensures(...)
#endif

/* do not change the following! */
#ifdef DRIVER
/* create aliases for driver tests */
#define malloc mm_malloc
#define free mm_free
#define realloc mm_realloc
#define calloc mm_calloc
#define memset mem_memset
#define memcpy mem_memcpy
#endif /* def DRIVER */

/* define the pow of 2 as an alignement */
/* here is 2^4=16 */
#define ALIGNMENT_POW 4
// define mask to retrieve size value
#define SIZEMASK 0xFFFFFFFFFFFFFFF8						
// flag to indicate if a block is 16-byte
#define MODE 0x4															
// flag to indicate if prev block is allocated
#define PREVALLOC 0x2													
// flag to indicate if current block is allocated
#define ALLOC 0x1															

// maximun chunk that need to extend
#define CHUNKSIZE_TINY 0x100									
// normal chunk size
#define CHUNKSIZE_NORMAL 0x1000								
// large chunksize
#define CHUNKSIZE_LARGE 0x8000								
// maximum small bin size
#define MAX_SMALL_BIN_SIZE 512								
// define maximum length of small bin list
#define MAX_SMALL_BIN_INDEX 32								
// define maximum length of big bin list
#define MAX_BIG_BIN_INDEX 32									
// define maximum size of fast bin buffer
#define MAX_FREE_BLOCK_BUFFER_SIZE 20					

/* Basic constants */
// 8 bytes in a x86-64 machine
typedef uint64_t word_t;											
// word, header, footer size (bytes)
static const size_t wsize = sizeof(word_t);   
// double word size (bytes)
static const size_t dsize = 2 * wsize;        
// Minimum block size - 16bytes here
static const size_t min_block_size = dsize; 	
// define non-valid index
static const int no_valid = 1 << 30;					
// buffer size at the beginning of heap			
// should ensure this value is multiple of 1static const int no_valid = 1<<30;
// here we set it equal to 32 + 32 = 64			
static const int heap_buffer_size = MAX_SMALL_BIN_INDEX + MAX_BIG_BIN_INDEX;	

/* Header mode: mode | prev alloc | alloc */
/* e.g.: 110- current block is small block |	prev block is allocated 
  			| current block isn't allocated */
typedef struct block {
	/* Header contains size + flag */
	word_t header;
	/*
	 * We don't know how big the payload will be.  Declaring it as an
	 * array of size 0 allows computing its starting address using
	 * pointer notation.
	 */
	char payload[0];
	/*
	 * We can't declare the footer as part of the struct, since its starting
	 * position is unknown
	 */

	/* Pointer to the prev free block */
	/* If no free block before, this pointer is set to NULL */
	struct block * prev;
	/* Pointer to the next free block */
	struct block * next;
} block_t;

/* 16-byte small block */
typedef struct small_block {
	/* if alloc, then prev is header, next is payload */
	/* if not, then prev is a pointer to prev 16-byte block with flag bits*/
	/* e.g.
	 *	|----------61 bits pointer---------|---3 bits flag--|
	 *																		 | mode | prev alloc | alloc|
	 * */
	struct small_block *prev;

	struct small_block *next;
} block_s;

/* Global variables */
/* Pointer to the heap space */
static word_t *heap = NULL;
/* Pointer to the prologue block */
static block_t *heap_prologue = NULL;
/* Pointer to the epilogue block */
static block_t *heap_epilogue = NULL;
/* Using bit map to indicate which bin has nodes */
static size_t bitMap = 0;
/* Free block list header */
static block_s * free_block_list_header[1];
/* Free block list tail */
static block_s * free_block_list_tail[1];
/* Number of fast bin */
static int free_block_count = 0;
/* Block that splits in the last operation */
static block_t * last_split_block = NULL;


/* Function prototypes for internal helper routines */
/************************ Business Function Start *****************************/
static block_t *extend_heap(size_t size);
static void place(block_t *block, size_t asize, size_t csize);
static block_t *coalesce(block_t * block, size_t size);
static block_t *unship_free_block(block_t *block);
static void insert_free_block(block_t *block, size_t size);
static int get_index(size_t size);
static int find_next_valid_index (int index);
static block_s * check_fast_list ();
static block_t * check_small_bin (size_t size);
static block_t * best_fit_search(size_t size);
static void add_into_free_list(block_s *bs);
static void clear_fast_list();
/************************ Business Function End *******************************/

/************************ Helper Function Start *******************************/
static word_t pack(size_t size, size_t flag);
static size_t align(size_t x);
static size_t max(size_t a, size_t b);
static bool is_alloc(block_t *block);
static bool is_prev_alloc(block_t * block);
static bool is_tiny_block(block_t * block);
static block_t *payload_to_header(void *bp);
static void *header_to_payload(block_t *block);
static block_t * header_to_footer(block_t * block);
static void write_header(block_t *block, size_t size, size_t flag);
static void write_footer(block_t *block);
static void write_block(block_t *block, size_t size, size_t flag);
static block_s * get_pointer(block_s * p);
static block_s * pack_pointer(block_s * src, block_s * tar);
static size_t get_size(block_t *block);
static word_t get_payload_size(block_t *block);
static size_t get_flag(block_t * block);
static void set_size(block_t *block, size_t size);
static void set_flag(block_t * block, bool set, size_t flag);
static block_t *find_next(block_t *block);
static block_t *find_prev(block_t *block);
/************************ Helper Function End ********************************/

/************************ Debug Function Start *******************************/
bool mm_checkheap(int lineno);
static bool check_pointer_boundary(void * p, void * start, void * end);
static void printBlock(block_t * block);
static void printHeap();
/************************ Debug Function End ********************************/


/*
 * Initialize: return false on error, true on success.
 */
bool mm_init(void) {

	// Create the initial empty heap 
	// buffer size + prologue + epilogue
	size_t isize = (heap_buffer_size + 2) * wsize;
	size_t chunksize = CHUNKSIZE_LARGE;
	// heap buffer space layout:
	// heap[0]: 16 bytes small block
	// heap[1]: 32 bytes small block
	// .......
	// heap[31]: 512 bytes small block
	// heap[32]: 528, 544 ~ 988 bytes block
	// heap[33]: variable-length block
	// ......
	// heap[63]: variable-length block
	// prologue: 8 bytes
	// payload: initilize chunsize 
	// epilogue: 8 bytes
	heap = (word_t *)(mem_sbrk(isize + chunksize));

	if (heap == (void *)-1) 
	{
		return false;
	}

	// Initialize bin pointers array to NULL pointers
	memset((void *)heap, 0, isize);
	heap[heap_buffer_size] = pack(0, PREVALLOC | ALLOC); // Prologue footer
	// Epilogue header
	heap[heap_buffer_size + (chunksize >>3) + 1] = pack(0, ALLOC); 
	// set global prologue and epilogue
	heap_prologue = (block_t *) &(heap[heap_buffer_size]);
	heap_epilogue = (block_t *) &(heap[heap_buffer_size + (chunksize >> 3)+ 1]);

	// set initialized free block
	block_t * block = (block_t *)((void *)heap_prologue + wsize);
	write_block(block, chunksize, PREVALLOC);
	last_split_block = block;

	// initialize free pointer array
	free_block_list_header[0] = NULL;
	free_block_list_tail[0] = NULL;

	dbg_printHeap();

	return true;
}

/*
 * malloc
 */
void *malloc (size_t size) {

	dbg_printf("Request malloc %lu bytes space\n", size);
	block_t *block = NULL;

	if (size <= 0) // Ignore spurious request
	{
		dbg_printf("Malloc(%d) --> %p\n", 0, NULL);
		return block;
	}

	// adjust block size to alignment
	size_t asize = align(size + wsize);	

	// check fast array
	if (asize==min_block_size && 
			(block = (block_t *)check_fast_list()) ) {
		return header_to_payload(block);
	}

	// check small bin
	if (asize <= MAX_SMALL_BIN_SIZE) {
		if ( (block = check_small_bin(asize))){
			place(block, asize, asize);
			return header_to_payload(block);
		} else
			clear_fast_list();
	}

	// check last split block
	if (last_split_block != NULL) {
		size_t last_size = get_size(last_split_block);
		block = last_split_block; 
		if (last_size >= asize) {
			place(block, asize, last_size);
			return header_to_payload(block);
		}
		else {
			// if last split block cannot match request
			// store it into free list
			insert_free_block(last_split_block, last_size);
			last_split_block = NULL;
		}
	}

	// not match in previous step, best fit here
	if ( (block = best_fit_search(asize))) {
		place(block, asize, get_size(block));
		return header_to_payload(block);
	}

	// not found fit size, then extend heap
	size_t exsize = asize; 	

	// get the last block of heap, if it's a free block
	// then just apply a new block with size of (size-payload_size(last block))
	bool prev_alloc = is_prev_alloc(heap_epilogue);
	if(!prev_alloc) {
		// last block has free space
		// allocate larger piece of memory, to avoid apply for new space frequently
		exsize = asize - get_size(find_prev(heap_epilogue));
	}
		
	exsize = exsize < CHUNKSIZE_TINY ? CHUNKSIZE_TINY : 
					 exsize < CHUNKSIZE_NORMAL? CHUNKSIZE_NORMAL :
					 exsize;

	//exsize = max(exsize, CHUNKSIZE_TINY);
	dbg_printf("No fit free block, allocate new space %lu\n", exsize);

	if ( (block = extend_heap(exsize)) ) {
		if (!prev_alloc)
			block = coalesce(block, exsize);
		place(block, asize, get_size(block));
		return header_to_payload(block);
	}

	dbg_printHeap();
	return NULL;
}

/*
 * free
 */
void free (void *ptr) {

	if(!ptr)
		return;

	dbg_printf("Free ptr: %p\n", ptr);

	block_t *block = payload_to_header(ptr);

	size_t size = get_size(block);

	// if it's tiny block, put it into fast bin
	if (size == min_block_size) 
		add_into_free_list((block_s *)block);
	else {
		// relieve immediately	
		// write header and footer
		set_flag(block, false, ALLOC);
		set_flag(find_next(block), false, PREVALLOC);
		block = coalesce(block, size);
		insert_free_block(block, get_size(block));
	}
	
	dbg_printf("Completed free(%p) with size:%lu\n", block, get_size(block));
	dbg_printHeap();
}

/*
 * realloc
 */
void *realloc(void *ptr, size_t size) {
	block_t *block = payload_to_header(ptr);
	size_t copysize;
	void *newptr;

	// If size == 0, then free block and return NULL
	if (size == 0)
	{
		free(ptr);
		return NULL;
	}

	// If ptr is NULL, then equivalent to malloc
	if (ptr == NULL)
	{
		return malloc(size);
	}

	// Otherwise, proceed with reallocation
	newptr = malloc(size);
	// If malloc fails, the original block is left untouched
	if (!newptr)
	{
		return NULL;
	}

	// Copy the old data
	copysize = get_payload_size(block); // gets size of old payload
	if(size < copysize)
	{
		copysize = size;
	}
	memcpy(newptr, ptr, copysize);

	// Free the old block
	free(ptr);

	return newptr;
}

/*
 * calloc
 * This function is not tested by mdriver
 */
void *calloc (size_t nmemb, size_t size) {
	void *bp;
	size_t asize = nmemb * size;

	if (asize/nmemb != size)
		// Multiplication overflowed
		return NULL;

	bp = malloc(asize);
	if (bp == NULL)
	{
		return NULL;
	}
	// Initialize all bits to 0
	memset(bp, 0, asize);

	return bp;
}

/************************** Business Function Start **************************/
/*
 * extend_heap: Extends the heap with the requested number of bytes, and
 *              recreates epilogue header. Returns a pointer to the result of
 *              coalescing the newly-created block with previous free block, if
 *              applicable, or NULL in failure.
 */
static block_t *extend_heap(size_t asize) {
	void *bp;

	// Allocate extra space which has at least minimum size of block
	// size = align(size + dsize);
	if ((bp = mem_sbrk(asize)) == (void *)-1)
	{
		return NULL;
	}

	// Initialize free block header/footer 
	// return the pointer of former epilogue block
	block_t *block = payload_to_header(bp);
	// now the header is actually the old epilogue
	// Set as free block
	write_block(block, asize, get_flag(block) & ~ALLOC);
	// Create new epilogue header
	block_t *block_next = find_next(block);
	write_header(block_next, 0, ALLOC);
	heap_epilogue = block_next;
	
	// return pointer to the new allocated space
	// notice we DO NOT coalesce new block with previous block
	// here. So there may exists two free blocks at this moment
	return block;
}

/*
 * place: Places block with size of asize at the start of bp. If the remaining
 *        size is at least the minimum block size, then split the block to 
 *        the allocated block and the remaining block as free, and then set it
 *        as last_split_block. Regard block must be a free block, so no need to
 *        check it here
 */
static void place(block_t *block, size_t asize, size_t csize) {
	
	size_t diff = csize - asize;

	if (diff >= min_block_size)
	{
		// split
		if (asize == min_block_size) {
			// small block
			set_flag(block, true, MODE | ALLOC);
		} else {
			set_flag(block, true, ALLOC);
			set_size(block, asize);
		}

		block_t * block_next = find_next(block);
		if (diff == min_block_size) {
			// next block is also 16-byte block
			write_block(block_next, diff, MODE | PREVALLOC);
		} else 
			write_block(block_next, diff, PREVALLOC);
		
		// store last_split_block into free list, if is different from block
		if (last_split_block && block != last_split_block) 
			insert_free_block(last_split_block, get_size(last_split_block));

		last_split_block = block_next;
	}

	else { 
		// no need to split
		size_t flag = asize == min_block_size ? MODE | ALLOC : ALLOC;
		set_flag(block, true, flag);
		set_flag(find_next(block), true, PREVALLOC);
		// if last_split_block is just block, set it to NULL
		if (last_split_block == block)
			last_split_block = NULL;
	}
}

/* Coalesce: Coalesces current block with previous and next blocks if
 *           either or both are unallocated; otherwise the block is not
 *           modified. Then, insert coalesced block into the segregated list.
 *           Returns pointer to the coalesced block. After coalescing, the
 *           immediate contiguous previous and next blocks must be allocated.
 */
static block_t *coalesce(block_t * block, size_t size) {

	dbg_printf("Coalesce block: %p\n", block);
	block_t *block_next = find_next(block);
	block_t *block_prev;

	bool if_prev_alloc = is_prev_alloc(block);
	bool if_next_alloc = is_alloc(block_next);

	if (if_prev_alloc && if_next_alloc)              // Case 1
	{
		dbg_printf("Case 1\n");
		// do nothing here
	}

	else if (if_prev_alloc && !if_next_alloc)        // Case 2
	{
		dbg_printf("Case 2\n");
		// if block_next is last_split_block, no need to unship it
		// cuz last_split_block isn't in the segregated list
		if (block_next != last_split_block)
			// unship block_next
			unship_free_block(block_next);
		else
			// coalesce last_split_block
			last_split_block = NULL;

		size += get_size(block_next);
		write_block(block, size, get_flag(block) & ~MODE);
	}

	else if (!if_prev_alloc && if_next_alloc)        // Case 3
	{	
		dbg_printf("Case 3\n");
		// prev block isn't allocated, so it has footer
		block_prev = find_prev(block);
		if (block_prev != last_split_block)
			// unship block_prev
			unship_free_block(block_prev);
		else
			last_split_block = NULL;

		size += get_size(block_prev);
		write_block(block_prev, size, get_flag(block_prev) & ~MODE);
		block = block_prev;
	}

	else                                        // Case 4
	{
		dbg_printf("Case 4\n");
		block_prev = find_prev(block);
		if (block_prev != last_split_block)
			// unship block_prev
			unship_free_block(block_prev);
		else
			last_split_block = NULL;
		if (block_next != last_split_block)
			// unship block_next
			unship_free_block(block_next);
		else
			last_split_block = NULL;

		size += get_size(block_next) + get_size(block_prev);
		write_block(block_prev, size, get_flag(block_prev) & ~MODE);
		block = block_prev;
	}

	if (last_split_block) {
		// store last_split_block
		insert_free_block(last_split_block, get_size(last_split_block));
		last_split_block = NULL;
	}
	return block;
}

/*
 * unship_free_block: unship the specific free block from list
 *
 */
static block_t * unship_free_block(block_t *block) {

	dbg_printf("Unship index:%d ", get_index(get_size((block_t *)block)));

	if (is_tiny_block(block)) {
		// 16-byte block
		block_s * bs = (block_s *)block;
		block_s * prev = get_pointer(bs->prev);
		block_s * next = get_pointer(bs->next);
		if( !prev ){
			// head node in the list
			heap[0] = (word_t) next;
			if ( !next )
				bitMap &= ~0x1L;
		} else {
			prev->next = pack_pointer(prev->next, next);
		}
		
		if( next )
			next->prev = pack_pointer(next->prev, prev);

	} else {
		// not small block
		if (!block->prev){
			// the first free block in the list
			int index = get_index(get_size(block));
			heap[index] = (word_t) block->next;
			if ( !block->next )
				bitMap &= ~(1L << index);
		} else {
			block->prev->next = block->next;
		}

		if(block->next)
			block->next->prev = block->prev;
		
	}

	dbg_printBlock((block_t *)block);
	return block;
}

/*
 * insert_free_block: insert a free block into specific bin according
 * 				to its size
 *
 */
static void insert_free_block(block_t * block, size_t size) {

	if (size == min_block_size) {
		// 16-byte block
		bitMap |= 0x1;
		block_s * bs = (block_s *) block;
		block_s * next = (block_s *) heap[0];
		heap[0] = (word_t)bs;
		bs->prev = pack_pointer(bs->prev, 0);
		bs->next = pack_pointer(bs->next, next);
		if (next)
			next->prev = pack_pointer(next->prev, bs);

	} else {
		// not small block
		int index = get_index(size);
		bitMap |= (1L << index);
		
		// small block bin, using LIFO
		if (index < MAX_SMALL_BIN_INDEX) {
			block_t * next = (block_t *)heap[index];
			block->prev = NULL;
			block->next = next;
			heap[index] = (word_t)block;
			if (next)
				next->prev = block;
		} 
		else {
			// big bin, using size-ordered
			block_t * start = (block_t *)heap[index];
			if (!start) {
				// empty bin, add to the header
				heap[index] = (word_t)block;
				block->prev = NULL;
				block->next = NULL;
				return;
			}
			// contains node
			while(start->next && get_size(start)<size)
				start = start->next;
			if (get_size(start) >= size) {
				// insert before start
				block->prev = start->prev;
				block->next = start;
				if (start->prev)
					start->prev->next = block;
				else 
					heap[index]	= (word_t) block;
				start->prev = block;
			} else {
				// insert after start
				block->prev = start;
				block->next = start->next;
				if (start->next)
					start->next->prev = block;
				start->next = block;
			}
		}
	}

}

/*
 * get_index: return index of free block pointers array
 * 						according to the value of size
 * 		
 *
 */
static int get_index(size_t size) {
	if (size <= MAX_SMALL_BIN_SIZE) {
		// small bin	
		// return size/16 - 1
		return (size >> ALIGNMENT_POW) - 1;

	} else {
		size >>= ALIGNMENT_POW;
		/*
		 * divide size according to its size
		 * size will b 2^9 ~ 2^64
		 *
		 * [2^9~2^10): 6 bins
		 * [2^10~2^11): 6 bins
		 * [2^11~2^12): 7 bins
		 * [2^12~2^13): 8 bins
		 * [2^13~2^64]: 5 bins
		 *
		 * Divide bins like this is basing on the frequency of different size,
		 * by analysing the appearence frequence of each size, I found that
		 * size of 2^4 ~ 2^9 would be the most frequency size, and less frequency
		 * size occurs within 2^9 ~ 2^13, after that would the least frequency block
		 * And I also notice that in the less frequency section, size between 
		 * 2^11~2^13 occurs more times than others, so I allocate more bins to them
		 *
		 */
		int index = 
			(size >> 9) ? (size >= 937 ? 32 : (((size - 512) >> 1) / 51 + 28))
				: (size >> 8) ? ( ((size - 256) >> 5) + 20) 
					:	(size >> 7) ? (((size - 128) >> 1) / 9 + 13) 
						: (size >> 6) ? (((size - 64) >> 1) / 5 + 7)
							: (size >> 5) ? ((size - 32) / 5 + 1)	
								: 0;

		return MAX_SMALL_BIN_INDEX + index - 1;
	}
}

/*
 * find_next_valid_index: find the next valid according to bitMap
 * 				if no used bit map, return a enough max number
 * 				here it's 1 << 30
 *
 */
static int find_next_valid_index (int index) {
	if (!(bitMap >> ++index))
		return no_valid;
	while( !((bitMap >> index) & 0x1) )
		++index;
	return index;
}

/*
 * check_fast_list: check if there is block in fast bin, if yes
 * 				return tail node. Here I use FIFO
 *
 */
static block_s * check_fast_list () {
	// regard index is in [0,3] here
	// do not check the boundary
	// small block	
	block_s * tail = free_block_list_tail[0];
	if (tail) {
		block_s * prev = get_pointer(tail->prev);
		free_block_list_tail[0] = prev;
		if (prev) {
			prev->next = pack_pointer(prev->next, 0);
		} else {
			// only one block, so set array to null
			// after removing it
			free_block_list_header[0] = NULL;
		}
		free_block_count--;
	}
	return tail;
}

/*
 * check_small_bin: check is there is block in small bin
 * 				meets the request
 *
 */
static block_t * check_small_bin (size_t size) {
	if (!bitMap)
		return NULL;

	int index = get_index(size);
	block_t * block = (block_t *)heap[index];
	if (block)
		return unship_free_block(block);
	return NULL;
}

/*
 * best_fit_search: Find a block which is best-fit with the size
 *
 */
static block_t * best_fit_search(size_t size) {
	if (!bitMap)
		return NULL;

	block_t * block = NULL;
	int index = get_index(size);

	// search in small bins
	if (index < MAX_SMALL_BIN_INDEX) {
		// increase index, because corresponding index must not contain a block
		for (++index; index<= MAX_SMALL_BIN_INDEX; 
				index = find_next_valid_index(index)) {
			if (heap[index])
				return unship_free_block((block_t *) heap[index]);
		}
	}

	// search in big bins
	for (; index < heap_buffer_size && !block; 
			index = find_next_valid_index(index)) {
		if (heap[index]) {
			block = (block_t *)heap[index];
			while(block && get_size(block)<size)
				block = block->next;
		}
	}

	return block ? unship_free_block(block) : NULL;
}

/*
 * add_into_free_list: add block into array, if array is full, 
 * 					store the tail element
 *
 */
static void add_into_free_list(block_s *bs) {
		// small block
		block_s *head = free_block_list_header[0];
		// insert into header
		free_block_list_header[0] = bs;
		bs->prev = pack_pointer(bs->prev, 0);
		if (!head) {
			// empty array
			free_block_list_tail[0] = bs;
			bs->next = pack_pointer(bs->next, 0);
			free_block_count = 1;
		} else {
			// not empty
			bs->next = pack_pointer(bs->next, head);
			head->prev = pack_pointer(head->prev, bs);
			if (free_block_count == MAX_FREE_BLOCK_BUFFER_SIZE) {
				// list is full
				// remove tail and store
				block_s *tail = free_block_list_tail[0];
				block_s *prev = get_pointer(tail->prev);
				free_block_list_tail[0] = prev;
				prev->next = pack_pointer(prev->next, 0); 
				set_flag((block_t *)tail, false, ALLOC);
				set_flag(find_next((block_t *)tail), false, PREVALLOC);
				block_t *block = coalesce((block_t *)tail, min_block_size);
				insert_free_block(block, get_size(block));
			} else
				free_block_count++;
		}
}

/*
 * clear_fast_list: clear all the elements in fast array, coalesce them
 * 					and store
 *
 */
static void clear_fast_list() {
	// clear index 0 first
	if (free_block_list_header[0]) {
		block_s * bs = free_block_list_header[0];
		free_block_list_header[0] = NULL;
		free_block_list_tail[0] = NULL;
		free_block_count = 0;

		while(bs) {
			set_flag((block_t *)bs, false, ALLOC);
			set_flag(find_next((block_t *)bs), false, PREVALLOC);
			block_s * next = get_pointer(bs->next);
			block_t * block = coalesce((block_t *)bs, 16);
			insert_free_block(block, get_size(block));
			bs = next;
		}
	}
}
/*********************** Business Function End *******************************/

/*********************** Helper Function Start *******************************/
/*
 * Return whether the pointer is aligned.
 */
static bool aligned(const void *p) {
	size_t ip = (size_t) p;
	return align(ip) == ip;
}

/*
 * pack: returns a header reflecting a specified size, and flag
 * 			 If the prev block is small block, the 4th lowest bit is 1, 
 * 			 and 0 otherwise.
 * 			 If the block is small block, the 3th lowest bit is 1, 
 * 			 and 0 otherwise.
 *       If prev block is allocated, the second lowest bit is set to 1, 
 *       and 0 otherwise
 *       If the block is allocated, the lowest bit is set to 1, and 0 otherwise.
 */
static word_t pack(size_t size, size_t flag) {
	return (size & SIZEMASK) | flag;
}

/* rounds up to the nearest multiple of ALIGNMENT */
static size_t align(size_t x) {
	return ( (x+ (1<<ALIGNMENT_POW)-1) >> ALIGNMENT_POW) << ALIGNMENT_POW; 
}

/*
 * is_alloc: return if a block has been allocated
 *
 */
static bool is_alloc(block_t *block){
	return (bool)(block->header & ALLOC);
}

/*
 * is_prev_alloc: return if prev block has been allocated 
 *
 */
static bool is_prev_alloc(block_t * block) {
	return (bool)(block->header & PREVALLOC);
}

/*
 * is_tiny_block: return if present block is small block
 *
 */
static bool is_tiny_block(block_t *block) {
	return (bool)(block->header & MODE);
}

/*
 * header_to_footer: get the footer of a free block according to its
 * 									 header pointer
 *
 */
static block_t * header_to_footer(block_t * block) {
	return (block_t *)(block->payload + get_size(block) - dsize);
}

/*
 * header_to_payload: given a block pointer, returns a pointer to the
 *                    corresponding payload.
 */
static void *header_to_payload(block_t *block) {
	return (void *)(block->payload);
}

/*
 * set_size: set the size of block in the header without 
 * 								 modifying other bits
 *
 */
static void set_size(block_t *block, size_t size) {
	block->header = (block->header & ~SIZEMASK) | size;
	// not alloc, then must have footer
	if( !(block->header & ALLOC))
		write_footer(block);
}

/*
 * set_flag: set flag to present block according to flag.
 * 					 If set, then set the flag to block
 * 					 otherwise set specifig bits to 0 according to flag
 *
 */
static void set_flag(block_t * block, bool set, size_t flag) {
	block->header = set ? (block->header | flag) : (block->header & ~flag);
	if( !(block->header & ALLOC))
		write_footer(block);
}

/*
 * get_flag: return flag of block
 *
 */
static size_t get_flag(block_t * block) {
	return block->header & ~SIZEMASK;
}

/*
 * payload_to_header: given a payload pointer, returns a pointer to the
 *                    corresponding block.
 */
static block_t *payload_to_header(void *bp) {
	return (block_t *)(((char *)bp) - offsetof(block_t, payload));
}

/*
 * write_header: given a block and its size, prev mode, mode, 
 * 							 prev allocation status, allocation status, writes an 
 * 							 appropriate value to the block header.
 *
 */
static void write_header(block_t *block, size_t size, size_t flag) {
	block->header = pack(size, flag);
}

/*
 * write_footer: given a block and its size, allocation status and 
 * 							 prev block allocation status, writes an appropriate 
 * 							 value to the block footer by first computing the 
 * 							 position of the footer.
 */
static void write_footer(block_t *block) {
	if (block->header & MODE) {
		// small block	
		word_t *footerp = (word_t *)(block->payload);
		*footerp = (*footerp & SIZEMASK) | (block->header & ~SIZEMASK);
	} else {
		word_t *footerp = (word_t *)((block->payload) + get_size(block) - dsize);
		*footerp = block->header;
	}
}

/*
 * write_block: write a free block with header and footer, 
 * 							which equals to invoke write_header() and write_footer()
 *
 */
static void write_block(block_t *block, size_t size, size_t flag) {
	write_header(block, size, flag);
	write_footer(block);
}

/*
 * find_next: returns the next consecutive block on the heap by adding the
 *            size of the block.
 */
static block_t *find_next(block_t *block) {
	return (block_t *)( (void *)block + get_size(block));
}

/*
 * find_prev: returns the previous block position by checking the previous
 *            block's footer and calculating the start of the previous block
 *            based on its size.
 */
static block_t *find_prev(block_t *block) {
	block_t * footerp = (block_t *)( (&(block->header)) - 1);
	size_t size = get_size(footerp);
	return (block_t *)((void *)block - size);
}

/*
 * get_size: returns the size of a given block by clearing the lowest 4 bits
 *           (as the heap is 16-byte aligned).
 */
static size_t get_size(block_t *block) {
	return !(block->header & MODE) ? (block->header & SIZEMASK) : min_block_size;
}

/*
 * get_payload_size: returns the payload size of a given block, because we
 * 									 use explicit block, so it equal to the entire block size 
 * 									 minus the header sizes
 */
static word_t get_payload_size(block_t *block) {
	return get_size(block) - wsize;
}

/*
 * max: return the larger one
 *
 */
static size_t max(size_t a, size_t b) {
	return a > b ? a : b;
}

/*
 * pack_pointer: Because small block store control bits in the pointer area,
 * 						it cannot directly assign value as a pointer. Use this transform
 * 						function to give tar pointer to src pointer area
 *
 */
static block_s * pack_pointer(block_s * src, block_s * tar) {
	return (block_s *) ((word_t)tar | ((word_t)src & ~SIZEMASK));
}

/*
 * get_pointer: return prev or next pointer of small block
 *
 */
static block_s * get_pointer(block_s * p) {
	return (block_s *) ((word_t)p & SIZEMASK);
}
/********************* Helper Function End *********************/

/********************* Debug Function Start *********************/
/*
 * check_pointer_boundary: check if pointer p locates between start and end
 *
 */
static bool check_pointer_boundary(void * p, void * start, void * end){
	return p >= start && p <= end;
}

/*
 * printBlock: print the detail of a block
 *
 */
static void printBlock(block_t * block) {
	if(block == NULL){
		printf("NULL block\n");
		return;
	}
	size_t size = get_size(block);
	size_t alloc = is_alloc(block);
	size_t flag = get_flag(block);

	printf("Block address:%p ", block);
	
	if(block == heap_prologue)
		printf("Prologue block\n");

	else if(block == heap_epilogue)
		printf("Epilogue block, small:%d, prev_alloc:%d, alloc:%d\n", 
				(bool)(flag & MODE), (bool)(flag & PREVALLOC), (bool)(flag & ALLOC));

	else if(size>=min_block_size){
		if(alloc) {
			printf("Allocated block, block_size:%lu bytes, small:%d, ", 
					size,	(bool)(flag & MODE));
			printf("prev_alloc:%d, alloc:%d\n", 
					(bool)(flag & PREVALLOC), (bool)(flag & ALLOC));
		}
		else {
			if(flag & MODE) {
				block_s * prev_free = (block_s *)((word_t)(block->header) & SIZEMASK);
				block_s * next_free = (block_s *)((word_t)(block->prev) & SIZEMASK);
				printf("Free block, size:%lu bytes, prev_free_block:%p, ", 
						size, prev_free);
				printf("next_free_block:%p, small:%d, prev_alloc:%d, alloc:%d\n", 
						next_free, (bool)(flag & MODE), (bool)(flag & PREVALLOC), 
						(bool)(flag & ALLOC));
			} else {
				printf("Free block, size:%lu bytes, prev_free_block:%p, ", size, 
						block->prev);
				printf("next_free_block:%p, small:%d, prev_alloc:%d, alloc:%d\n", 
						block->next, (bool)(flag & MODE), (bool)(flag & PREVALLOC), 
						(bool)(flag & ALLOC));
			}
		}
	} else
		printf("Illegal block type!\n");

	return;
}

/*
 * printHeap: print all information in the heap
 *
 */
static void printHeap(){
	printf("*********************************************\n");
	printf("FREE BLOCK POINTERS ARRAY:\n");
	for(int i=0; i<heap_buffer_size; ++i){
		printf("%d %lx\n", i, *(heap+i));
	}

	printf("HEAP BLOCKS :\n");
	printBlock(heap_prologue);
	block_t * start = (block_t *)((void *)heap_prologue + wsize);
	while(true){
		printBlock(start);
		block_t * next = find_next(start);
		if(next == start)
			break;
		start = next;
	}
	printf("*********************************************\n");

}

/*
 * mm_checkheap
 */
bool mm_checkheap(int lineno) {
	void * heap_start = mem_heap_lo();
	void * heap_end = mem_heap_hi();
	block_t * prologue = heap_prologue;
	block_t * epilogue = heap_epilogue;
	int count_free = 0;

	// check the prologue and epilogue blocks
	dbg_assert(is_alloc(prologue) && !get_size(prologue));
	dbg_assert(is_alloc(epilogue) && !get_size(epilogue));
	dbg_assert(check_pointer_boundary((void *)prologue, heap_start, heap_end));
	dbg_assert(check_pointer_boundary((void *)epilogue, heap_start, heap_end));
	dbg_assert(aligned((void *)prologue));

	// check the heap block one by one
	block_t *now = (block_t *)((word_t *)prologue + 1);
	while(now != epilogue){
		void * last_pointer = (void *)now + get_size(now) - 1;
		dbg_assert(check_pointer_boundary((void *)now, heap_start, heap_end));
		dbg_assert(check_pointer_boundary(last_pointer, heap_start, heap_end));
		dbg_assert(aligned((void *) header_to_payload(now)));
		// if it's free block, then header == footer
		dbg_assert(
					is_alloc(now) || is_tiny_block(now)
						|| now->header == *( (word_t *)(header_to_footer(now)) )
				);

		block_t *next = find_next(now);
		// the prev_alloc of next block should match
		dbg_assert(is_alloc(now) == is_prev_alloc(next));
		// no consecutive free blocks
		dbg_assert(is_alloc(now) | is_alloc(next));

		//count the number of free blocks
		count_free = is_alloc(now) ? count_free : count_free + 1;

		now = next;
	}
	
	// check segragated free lists
	for(int i=0; i<heap_buffer_size; ++i){
		if (heap[i] == 0)
			continue;

		// contains free list
		now = (block_t *) *(heap + i);
		// ensure it's a free block
		dbg_assert(!is_alloc(now));
		// check pointer boundary
		void * last_pointer = (void *)now + get_size(now) - 1;
		dbg_assert(check_pointer_boundary((void *)now, heap_start, heap_end));
		dbg_assert(check_pointer_boundary(last_pointer, heap_start, heap_end));
		// check aligned
		dbg_assert(aligned((void *) header_to_payload(now)));
		// check if the bucket size match
		if(i != get_index(get_size(now))){
			printf("pointer:%p, lineno:%d, i=%d, index=%d\n", 
					now, lineno, i, get_index(get_payload_size(now)));
			printHeap();
		}
		dbg_assert(i == get_index(get_size(now)));
		while(now){
			if(is_tiny_block(now)) 
				now = (block_t *)get_pointer(((block_s *)now)->next);
			else
				now = now->next;
			--count_free;
		}
	}

	dbg_assert(count_free <= 1);
	
	return true;
}
/************************ Debug Function End ***********************/

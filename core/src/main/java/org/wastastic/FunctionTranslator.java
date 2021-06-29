package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCMPG;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IOR;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.ISHL;
import static org.objectweb.asm.Opcodes.ISHR;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.IUSHR;
import static org.objectweb.asm.Opcodes.IXOR;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2F;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.V17;
import static org.wastastic.CodegenUtils.pushF32Constant;
import static org.wastastic.CodegenUtils.pushF64Constant;
import static org.wastastic.CodegenUtils.pushI32Constant;
import static org.wastastic.CodegenUtils.pushI64Constant;
import static org.wastastic.InstructionImpls.F32_CONVERT_I64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F32_CONVERT_I64_U_NAME;
import static org.wastastic.InstructionImpls.F32_TRUNC_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F32_TRUNC_NAME;
import static org.wastastic.InstructionImpls.F64_CONVERT_I64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F64_CONVERT_I64_U_NAME;
import static org.wastastic.InstructionImpls.F64_TRUNC_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F64_TRUNC_NAME;
import static org.wastastic.InstructionImpls.I32_DIV_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_DIV_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F32_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F64_U_NAME;
import static org.wastastic.InstructionImpls.I64_DIV_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_DIV_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F32_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F64_U_NAME;
import static org.wastastic.Lists.first;
import static org.wastastic.Lists.last;
import static org.wastastic.Lists.removeLast;
import static org.wastastic.Names.DOUBLE_INTERNAL_NAME;
import static org.wastastic.Names.FLOAT_INTERNAL_NAME;
import static org.wastastic.Names.FUNCTION_CLASS_ENTRY_NAME;
import static org.wastastic.Names.GENERATED_FUNCTION_INTERNAL_NAME;
import static org.wastastic.Names.INTEGER_INTERNAL_NAME;
import static org.wastastic.Names.LONG_INTERNAL_NAME;
import static org.wastastic.Names.MATH_INTERNAL_NAME;
import static org.wastastic.Names.MEMORY_SEGMENT_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_INTERNAL_NAME;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_INTERNAL_NAME;
import static org.wastastic.WasmOpcodes.OP_BLOCK;
import static org.wastastic.WasmOpcodes.OP_BR;
import static org.wastastic.WasmOpcodes.OP_BR_IF;
import static org.wastastic.WasmOpcodes.OP_BR_TABLE;
import static org.wastastic.WasmOpcodes.OP_CALL;
import static org.wastastic.WasmOpcodes.OP_CALL_INDIRECT;
import static org.wastastic.WasmOpcodes.OP_CONT_DATA_DROP;
import static org.wastastic.WasmOpcodes.OP_CONT_ELEM_DROP;
import static org.wastastic.WasmOpcodes.OP_CONT_I32_TRUNC_SAT_F32_S;
import static org.wastastic.WasmOpcodes.OP_CONT_I32_TRUNC_SAT_F32_U;
import static org.wastastic.WasmOpcodes.OP_CONT_I32_TRUNC_SAT_F64_S;
import static org.wastastic.WasmOpcodes.OP_CONT_I32_TRUNC_SAT_F64_U;
import static org.wastastic.WasmOpcodes.OP_CONT_I64_TRUNC_SAT_F32_S;
import static org.wastastic.WasmOpcodes.OP_CONT_I64_TRUNC_SAT_F32_U;
import static org.wastastic.WasmOpcodes.OP_CONT_I64_TRUNC_SAT_F64_S;
import static org.wastastic.WasmOpcodes.OP_CONT_I64_TRUNC_SAT_F64_U;
import static org.wastastic.WasmOpcodes.OP_CONT_MEMORY_COPY;
import static org.wastastic.WasmOpcodes.OP_CONT_MEMORY_FILL;
import static org.wastastic.WasmOpcodes.OP_CONT_MEMORY_INIT;
import static org.wastastic.WasmOpcodes.OP_CONT_PREFIX;
import static org.wastastic.WasmOpcodes.OP_CONT_TABLE_COPY;
import static org.wastastic.WasmOpcodes.OP_CONT_TABLE_FILL;
import static org.wastastic.WasmOpcodes.OP_CONT_TABLE_GROW;
import static org.wastastic.WasmOpcodes.OP_CONT_TABLE_INIT;
import static org.wastastic.WasmOpcodes.OP_CONT_TABLE_SIZE;
import static org.wastastic.WasmOpcodes.OP_DROP;
import static org.wastastic.WasmOpcodes.OP_ELSE;
import static org.wastastic.WasmOpcodes.OP_END;
import static org.wastastic.WasmOpcodes.OP_F32_ABS;
import static org.wastastic.WasmOpcodes.OP_F32_ADD;
import static org.wastastic.WasmOpcodes.OP_F32_CEIL;
import static org.wastastic.WasmOpcodes.OP_F32_CONST;
import static org.wastastic.WasmOpcodes.OP_F32_CONVERT_I32_S;
import static org.wastastic.WasmOpcodes.OP_F32_CONVERT_I32_U;
import static org.wastastic.WasmOpcodes.OP_F32_CONVERT_I64_S;
import static org.wastastic.WasmOpcodes.OP_F32_CONVERT_I64_U;
import static org.wastastic.WasmOpcodes.OP_F32_COPYSIGN;
import static org.wastastic.WasmOpcodes.OP_F32_DEMOTE_F64;
import static org.wastastic.WasmOpcodes.OP_F32_DIV;
import static org.wastastic.WasmOpcodes.OP_F32_EQ;
import static org.wastastic.WasmOpcodes.OP_F32_FLOOR;
import static org.wastastic.WasmOpcodes.OP_F32_GE;
import static org.wastastic.WasmOpcodes.OP_F32_GT;
import static org.wastastic.WasmOpcodes.OP_F32_LE;
import static org.wastastic.WasmOpcodes.OP_F32_LOAD;
import static org.wastastic.WasmOpcodes.OP_F32_LT;
import static org.wastastic.WasmOpcodes.OP_F32_MAX;
import static org.wastastic.WasmOpcodes.OP_F32_MIN;
import static org.wastastic.WasmOpcodes.OP_F32_MUL;
import static org.wastastic.WasmOpcodes.OP_F32_NE;
import static org.wastastic.WasmOpcodes.OP_F32_NEAREST;
import static org.wastastic.WasmOpcodes.OP_F32_NEG;
import static org.wastastic.WasmOpcodes.OP_F32_REINTERPRET_I32;
import static org.wastastic.WasmOpcodes.OP_F32_SQRT;
import static org.wastastic.WasmOpcodes.OP_F32_STORE;
import static org.wastastic.WasmOpcodes.OP_F32_SUB;
import static org.wastastic.WasmOpcodes.OP_F32_TRUNC;
import static org.wastastic.WasmOpcodes.OP_F64_ABS;
import static org.wastastic.WasmOpcodes.OP_F64_ADD;
import static org.wastastic.WasmOpcodes.OP_F64_CEIL;
import static org.wastastic.WasmOpcodes.OP_F64_CONST;
import static org.wastastic.WasmOpcodes.OP_F64_CONVERT_I32_S;
import static org.wastastic.WasmOpcodes.OP_F64_CONVERT_I32_U;
import static org.wastastic.WasmOpcodes.OP_F64_CONVERT_I64_S;
import static org.wastastic.WasmOpcodes.OP_F64_CONVERT_I64_U;
import static org.wastastic.WasmOpcodes.OP_F64_COPYSIGN;
import static org.wastastic.WasmOpcodes.OP_F64_DIV;
import static org.wastastic.WasmOpcodes.OP_F64_EQ;
import static org.wastastic.WasmOpcodes.OP_F64_FLOOR;
import static org.wastastic.WasmOpcodes.OP_F64_GE;
import static org.wastastic.WasmOpcodes.OP_F64_GT;
import static org.wastastic.WasmOpcodes.OP_F64_LE;
import static org.wastastic.WasmOpcodes.OP_F64_LOAD;
import static org.wastastic.WasmOpcodes.OP_F64_LT;
import static org.wastastic.WasmOpcodes.OP_F64_MAX;
import static org.wastastic.WasmOpcodes.OP_F64_MIN;
import static org.wastastic.WasmOpcodes.OP_F64_MUL;
import static org.wastastic.WasmOpcodes.OP_F64_NE;
import static org.wastastic.WasmOpcodes.OP_F64_NEAREST;
import static org.wastastic.WasmOpcodes.OP_F64_NEG;
import static org.wastastic.WasmOpcodes.OP_F64_PROMOTE_F32;
import static org.wastastic.WasmOpcodes.OP_F64_REINTERPRET_I64;
import static org.wastastic.WasmOpcodes.OP_F64_SQRT;
import static org.wastastic.WasmOpcodes.OP_F64_STORE;
import static org.wastastic.WasmOpcodes.OP_F64_SUB;
import static org.wastastic.WasmOpcodes.OP_F64_TRUNC;
import static org.wastastic.WasmOpcodes.OP_GLOBAL_GET;
import static org.wastastic.WasmOpcodes.OP_GLOBAL_SET;
import static org.wastastic.WasmOpcodes.OP_I32_ADD;
import static org.wastastic.WasmOpcodes.OP_I32_AND;
import static org.wastastic.WasmOpcodes.OP_I32_CLZ;
import static org.wastastic.WasmOpcodes.OP_I32_CONST;
import static org.wastastic.WasmOpcodes.OP_I32_CTZ;
import static org.wastastic.WasmOpcodes.OP_I32_DIV_S;
import static org.wastastic.WasmOpcodes.OP_I32_DIV_U;
import static org.wastastic.WasmOpcodes.OP_I32_EQ;
import static org.wastastic.WasmOpcodes.OP_I32_EQZ;
import static org.wastastic.WasmOpcodes.OP_I32_EXTEND16_S;
import static org.wastastic.WasmOpcodes.OP_I32_EXTEND8_S;
import static org.wastastic.WasmOpcodes.OP_I32_GE_S;
import static org.wastastic.WasmOpcodes.OP_I32_GE_U;
import static org.wastastic.WasmOpcodes.OP_I32_GT_S;
import static org.wastastic.WasmOpcodes.OP_I32_GT_U;
import static org.wastastic.WasmOpcodes.OP_I32_LE_S;
import static org.wastastic.WasmOpcodes.OP_I32_LE_U;
import static org.wastastic.WasmOpcodes.OP_I32_LOAD;
import static org.wastastic.WasmOpcodes.OP_I32_LOAD16_S;
import static org.wastastic.WasmOpcodes.OP_I32_LOAD16_U;
import static org.wastastic.WasmOpcodes.OP_I32_LOAD8_S;
import static org.wastastic.WasmOpcodes.OP_I32_LOAD8_U;
import static org.wastastic.WasmOpcodes.OP_I32_LT_S;
import static org.wastastic.WasmOpcodes.OP_I32_LT_U;
import static org.wastastic.WasmOpcodes.OP_I32_MUL;
import static org.wastastic.WasmOpcodes.OP_I32_NE;
import static org.wastastic.WasmOpcodes.OP_I32_OR;
import static org.wastastic.WasmOpcodes.OP_I32_POPCNT;
import static org.wastastic.WasmOpcodes.OP_I32_REINTERPRET_F32;
import static org.wastastic.WasmOpcodes.OP_I32_REM_S;
import static org.wastastic.WasmOpcodes.OP_I32_REM_U;
import static org.wastastic.WasmOpcodes.OP_I32_ROTL;
import static org.wastastic.WasmOpcodes.OP_I32_ROTR;
import static org.wastastic.WasmOpcodes.OP_I32_SHL;
import static org.wastastic.WasmOpcodes.OP_I32_SHR_S;
import static org.wastastic.WasmOpcodes.OP_I32_SHR_U;
import static org.wastastic.WasmOpcodes.OP_I32_STORE;
import static org.wastastic.WasmOpcodes.OP_I32_STORE16;
import static org.wastastic.WasmOpcodes.OP_I32_STORE8;
import static org.wastastic.WasmOpcodes.OP_I32_SUB;
import static org.wastastic.WasmOpcodes.OP_I32_TRUNC_F32_S;
import static org.wastastic.WasmOpcodes.OP_I32_TRUNC_F32_U;
import static org.wastastic.WasmOpcodes.OP_I32_TRUNC_F64_S;
import static org.wastastic.WasmOpcodes.OP_I32_TRUNC_F64_U;
import static org.wastastic.WasmOpcodes.OP_I32_WRAP_I64;
import static org.wastastic.WasmOpcodes.OP_I32_XOR;
import static org.wastastic.WasmOpcodes.OP_I64_ADD;
import static org.wastastic.WasmOpcodes.OP_I64_AND;
import static org.wastastic.WasmOpcodes.OP_I64_CLZ;
import static org.wastastic.WasmOpcodes.OP_I64_CONST;
import static org.wastastic.WasmOpcodes.OP_I64_CTZ;
import static org.wastastic.WasmOpcodes.OP_I64_DIV_S;
import static org.wastastic.WasmOpcodes.OP_I64_DIV_U;
import static org.wastastic.WasmOpcodes.OP_I64_EQ;
import static org.wastastic.WasmOpcodes.OP_I64_EQZ;
import static org.wastastic.WasmOpcodes.OP_I64_EXTEND16_S;
import static org.wastastic.WasmOpcodes.OP_I64_EXTEND32_S;
import static org.wastastic.WasmOpcodes.OP_I64_EXTEND8_S;
import static org.wastastic.WasmOpcodes.OP_I64_EXTEND_I32_S;
import static org.wastastic.WasmOpcodes.OP_I64_EXTEND_I32_U;
import static org.wastastic.WasmOpcodes.OP_I64_GE_S;
import static org.wastastic.WasmOpcodes.OP_I64_GE_U;
import static org.wastastic.WasmOpcodes.OP_I64_GT_S;
import static org.wastastic.WasmOpcodes.OP_I64_GT_U;
import static org.wastastic.WasmOpcodes.OP_I64_LE_S;
import static org.wastastic.WasmOpcodes.OP_I64_LE_U;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD16_S;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD16_U;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD32_S;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD32_U;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD8_S;
import static org.wastastic.WasmOpcodes.OP_I64_LOAD8_U;
import static org.wastastic.WasmOpcodes.OP_I64_LT_S;
import static org.wastastic.WasmOpcodes.OP_I64_LT_U;
import static org.wastastic.WasmOpcodes.OP_I64_MUL;
import static org.wastastic.WasmOpcodes.OP_I64_NE;
import static org.wastastic.WasmOpcodes.OP_I64_OR;
import static org.wastastic.WasmOpcodes.OP_I64_POPCNT;
import static org.wastastic.WasmOpcodes.OP_I64_REINTERPRET_F64;
import static org.wastastic.WasmOpcodes.OP_I64_REM_S;
import static org.wastastic.WasmOpcodes.OP_I64_REM_U;
import static org.wastastic.WasmOpcodes.OP_I64_ROTL;
import static org.wastastic.WasmOpcodes.OP_I64_ROTR;
import static org.wastastic.WasmOpcodes.OP_I64_SHL;
import static org.wastastic.WasmOpcodes.OP_I64_SHR_S;
import static org.wastastic.WasmOpcodes.OP_I64_SHR_U;
import static org.wastastic.WasmOpcodes.OP_I64_STORE;
import static org.wastastic.WasmOpcodes.OP_I64_STORE16;
import static org.wastastic.WasmOpcodes.OP_I64_STORE32;
import static org.wastastic.WasmOpcodes.OP_I64_STORE8;
import static org.wastastic.WasmOpcodes.OP_I64_SUB;
import static org.wastastic.WasmOpcodes.OP_I64_TRUNC_F32_S;
import static org.wastastic.WasmOpcodes.OP_I64_TRUNC_F32_U;
import static org.wastastic.WasmOpcodes.OP_I64_TRUNC_F64_S;
import static org.wastastic.WasmOpcodes.OP_I64_TRUNC_F64_U;
import static org.wastastic.WasmOpcodes.OP_I64_XOR;
import static org.wastastic.WasmOpcodes.OP_IF;
import static org.wastastic.WasmOpcodes.OP_LOCAL_GET;
import static org.wastastic.WasmOpcodes.OP_LOCAL_SET;
import static org.wastastic.WasmOpcodes.OP_LOCAL_TEE;
import static org.wastastic.WasmOpcodes.OP_LOOP;
import static org.wastastic.WasmOpcodes.OP_MEMORY_GROW;
import static org.wastastic.WasmOpcodes.OP_MEMORY_SIZE;
import static org.wastastic.WasmOpcodes.OP_NOP;
import static org.wastastic.WasmOpcodes.OP_REF_FUNC;
import static org.wastastic.WasmOpcodes.OP_REF_IS_NULL;
import static org.wastastic.WasmOpcodes.OP_REF_NULL;
import static org.wastastic.WasmOpcodes.OP_RETURN;
import static org.wastastic.WasmOpcodes.OP_SELECT;
import static org.wastastic.WasmOpcodes.OP_SELECT_VEC;
import static org.wastastic.WasmOpcodes.OP_TABLE_GET;
import static org.wastastic.WasmOpcodes.OP_TABLE_SET;
import static org.wastastic.WasmOpcodes.OP_UNREACHABLE;
import static org.wastastic.WasmOpcodes.TYPE_EXTERNREF;
import static org.wastastic.WasmOpcodes.TYPE_F32;
import static org.wastastic.WasmOpcodes.TYPE_F64;
import static org.wastastic.WasmOpcodes.TYPE_FUNCREF;
import static org.wastastic.WasmOpcodes.TYPE_I32;
import static org.wastastic.WasmOpcodes.TYPE_I64;

final class FunctionTranslator {
    private final List<ControlScope> controlStack = new ArrayList<>();
    private final List<ValueType> operandStack = new ArrayList<>();
    private final List<Local> locals = new ArrayList<>();

    private ParsedModule parsedModule;
    private WasmReader reader;
    private MethodVisitor functionWriter;

    private int instanceArgumentLocalIndex;
    private int firstScratchLocalIndex;

    byte @NotNull[] translate(@NotNull ParsedModule parsedModule, int functionIndex) throws TranslationException {
        this.parsedModule = parsedModule;

        reader = new WasmReader(parsedModule.functionBodies().get(functionIndex - parsedModule.importedFunctions().size()));

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(V17, ACC_FINAL, GENERATED_FUNCTION_INTERNAL_NAME, null, OBJECT_INTERNAL_NAME, null);

        functionWriter = classWriter.visitMethod(
            ACC_PRIVATE | ACC_STATIC,
            FUNCTION_CLASS_ENTRY_NAME,
            parsedModule.functionType(functionIndex).descriptor(),
            null,
            null
        );

        functionWriter.visitCode();

        var type = parsedModule.functionType(functionIndex);
        var nextLocalIndex = 0;

        for (var parameterType : type.parameterTypes()) {
            locals.add(new Local(parameterType, nextLocalIndex));
            nextLocalIndex += parameterType.width();
        }

        instanceArgumentLocalIndex = nextLocalIndex;
        nextLocalIndex += 1;

        for (var i = reader.nextUnsigned32(); i != 0; i--) {
            var fieldsRemaining = reader.nextUnsigned32();
            var fieldType = reader.nextValueType();
            for (; fieldsRemaining != 0; fieldsRemaining--) {
                locals.add(new Local(fieldType, nextLocalIndex));
                functionWriter.visitInsn(fieldType.zeroConstantOpcode());
                functionWriter.visitVarInsn(fieldType.localStoreOpcode(), nextLocalIndex);
                nextLocalIndex += fieldType.width();
            }
        }

        firstScratchLocalIndex = nextLocalIndex;

        var topScopeType = new FunctionType(List.of(), type.returnTypes());
        var functionScope = new BlockScope(new Label(), topScopeType, 0);
        controlStack.add(functionScope);

        while (!controlStack.isEmpty()) {
            translateInstruction();
        }

        if (!functionScope.restUnreachable()) {
            functionWriter.visitInsn(type.returnOpcode());
        }

        functionWriter.visitMaxs(0, 0);
        functionWriter.visitEnd();

        classWriter.visitEnd();
        return classWriter.toByteArray();
    }

    private void translateInstruction() throws TranslationException {
        var opcode = reader.nextByte();
        switch (opcode) {
            case OP_UNREACHABLE -> translateUnreachable();
            case OP_NOP -> translateNop();
            case OP_BLOCK -> translateBlock();
            case OP_LOOP -> translateLoop();
            case OP_IF -> translateIf();
            case OP_ELSE -> translateElse();
            case OP_END -> translateEnd();
            case OP_BR -> translateBr();
            case OP_BR_IF -> translateBrIf();
            case OP_BR_TABLE -> translateBrTable();
            case OP_RETURN -> translateReturn();
            case OP_CALL -> translateCall();
            case OP_CALL_INDIRECT -> translateCallIndirect();
            case OP_DROP -> translateDrop();
            case OP_SELECT -> translateSelect();
            case OP_SELECT_VEC -> translateSelectVec();
            case OP_LOCAL_GET -> translateLocalGet();
            case OP_LOCAL_SET -> translateLocalSet();
            case OP_LOCAL_TEE -> translateLocalTee();
            case OP_GLOBAL_GET -> translateGlobalGet();
            case OP_GLOBAL_SET -> translateGlobalSet();
            case OP_TABLE_GET -> translateTableGet();
            case OP_TABLE_SET -> translateTableSet();
            case OP_I32_LOAD -> translateI32Load();
            case OP_I64_LOAD -> translateI64Load();
            case OP_F32_LOAD -> translateF32Load();
            case OP_F64_LOAD -> translateF64Load();
            case OP_I32_LOAD8_S -> translateI32Load8S();
            case OP_I32_LOAD8_U -> translateI32Load8U();
            case OP_I32_LOAD16_S -> translateI32Load16S();
            case OP_I32_LOAD16_U -> translateI32Load16U();
            case OP_I64_LOAD8_S -> translateI64Load8S();
            case OP_I64_LOAD8_U -> translateI64Load8U();
            case OP_I64_LOAD16_S -> translateI64Load16S();
            case OP_I64_LOAD16_U -> translateI64Load16U();
            case OP_I64_LOAD32_S -> translateI64Load32S();
            case OP_I64_LOAD32_U -> translateI64Load32U();
            case OP_I32_STORE -> translateI32Store();
            case OP_I64_STORE -> translateI64Store();
            case OP_F32_STORE -> translateF32Store();
            case OP_F64_STORE -> translateF64Store();
            case OP_I32_STORE8 -> translateI32Store8();
            case OP_I32_STORE16 -> translateI32Store16();
            case OP_I64_STORE8 -> translateI64Store8();
            case OP_I64_STORE16 -> translateI64Store16();
            case OP_I64_STORE32 -> translateI64Store32();
            case OP_MEMORY_SIZE -> translateMemorySize();
            case OP_MEMORY_GROW -> translateMemoryGrow();
            case OP_I32_CONST -> translateI32Const();
            case OP_I64_CONST -> translateI64Const();
            case OP_F32_CONST -> translateF32Const();
            case OP_F64_CONST -> translateF64Const();
            case OP_I32_EQZ -> translateI32Eqz();
            case OP_I32_EQ -> translateI32Eq();
            case OP_I32_NE -> translateI32Ne();
            case OP_I32_LT_S -> translateI32LtS();
            case OP_I32_LT_U -> translateI32LtU();
            case OP_I32_GT_S -> translateI32GtS();
            case OP_I32_GT_U -> translateI32GtU();
            case OP_I32_LE_S -> translateI32LeS();
            case OP_I32_LE_U -> translateI32LeU();
            case OP_I32_GE_S -> translateI32GeS();
            case OP_I32_GE_U -> translateI32GeU();
            case OP_I64_EQZ -> translateI64Eqz();
            case OP_I64_EQ -> translateI64Eq();
            case OP_I64_NE -> translateI64Ne();
            case OP_I64_LT_S -> translateI64LtS();
            case OP_I64_LT_U -> translateI64LtU();
            case OP_I64_GT_S -> translateI64GtS();
            case OP_I64_GT_U -> translateI64GtU();
            case OP_I64_LE_S -> translateI64LeS();
            case OP_I64_LE_U -> translateI64LeU();
            case OP_I64_GE_S -> translateI64GeS();
            case OP_I64_GE_U -> translateI64GeU();
            case OP_F32_EQ -> translateF32Eq();
            case OP_F32_NE -> translateF32Ne();
            case OP_F32_LT -> translateF32Lt();
            case OP_F32_GT -> translateF32Gt();
            case OP_F32_LE -> translateF32Le();
            case OP_F32_GE -> translateF32Ge();
            case OP_F64_EQ -> translateF64Eq();
            case OP_F64_NE -> translateF64Ne();
            case OP_F64_LT -> translateF64Lt();
            case OP_F64_GT -> translateF64Gt();
            case OP_F64_LE -> translateF64Le();
            case OP_F64_GE -> translateF64Ge();
            case OP_I32_CLZ -> translateI32Clz();
            case OP_I32_CTZ -> translateI32Ctz();
            case OP_I32_POPCNT -> translateI32Popcnt();
            case OP_I32_ADD -> translateI32Add();
            case OP_I32_SUB -> translateI32Sub();
            case OP_I32_MUL -> translateI32Mul();
            case OP_I32_DIV_S -> translateI32DivS();
            case OP_I32_DIV_U -> translateI32DivU();
            case OP_I32_REM_S -> translateI32RemS();
            case OP_I32_REM_U -> translateI32RemU();
            case OP_I32_AND -> translateI32And();
            case OP_I32_OR -> translateI32Or();
            case OP_I32_XOR -> translateI32Xor();
            case OP_I32_SHL -> translateI32Shl();
            case OP_I32_SHR_S -> translateI32ShrS();
            case OP_I32_SHR_U -> translateI32ShrU();
            case OP_I32_ROTL -> translateI32Rotl();
            case OP_I32_ROTR -> translateI32Rotr();
            case OP_I64_CLZ -> translateI64Clz();
            case OP_I64_CTZ -> translateI64Ctz();
            case OP_I64_POPCNT -> translateI64Popcnt();
            case OP_I64_ADD -> translateI64Add();
            case OP_I64_SUB -> translateI64Sub();
            case OP_I64_MUL -> translateI64Mul();
            case OP_I64_DIV_S -> translateI64DivS();
            case OP_I64_DIV_U -> translateI64DivU();
            case OP_I64_REM_S -> translateI64RemS();
            case OP_I64_REM_U -> translateI64RemU();
            case OP_I64_AND -> translateI64And();
            case OP_I64_OR -> translateI64Or();
            case OP_I64_XOR -> translateI64Xor();
            case OP_I64_SHL -> translateI64Shl();
            case OP_I64_SHR_S -> translateI64ShrS();
            case OP_I64_SHR_U -> translateI64ShrU();
            case OP_I64_ROTL -> translateI64Rotl();
            case OP_I64_ROTR -> translateI64Rotr();
            case OP_F32_ABS -> translateF32Abs();
            case OP_F32_NEG -> translateF32Neg();
            case OP_F32_CEIL -> translateF32Ceil();
            case OP_F32_FLOOR -> translateF32Floor();
            case OP_F32_TRUNC -> translateF32Trunc();
            case OP_F32_NEAREST -> translateF32Nearest();
            case OP_F32_SQRT -> translateF32Sqrt();
            case OP_F32_ADD -> translateF32Add();
            case OP_F32_SUB -> translateF32Sub();
            case OP_F32_MUL -> translateF32Mul();
            case OP_F32_DIV -> translateF32Div();
            case OP_F32_MIN -> translateF32Min();
            case OP_F32_MAX -> translateF32Max();
            case OP_F32_COPYSIGN -> translateF32Copysign();
            case OP_F64_ABS -> translateF64Abs();
            case OP_F64_NEG -> translateF64Neg();
            case OP_F64_CEIL -> translateF64Ceil();
            case OP_F64_FLOOR -> translateF64Floor();
            case OP_F64_TRUNC -> translateF64Trunc();
            case OP_F64_NEAREST -> translateF64Nearest();
            case OP_F64_SQRT -> translateF64Sqrt();
            case OP_F64_ADD -> translateF64Add();
            case OP_F64_SUB -> translateF64Sub();
            case OP_F64_MUL -> translateF64Mul();
            case OP_F64_DIV -> translateF64Div();
            case OP_F64_MIN -> translateF64Min();
            case OP_F64_MAX -> translateF64Max();
            case OP_F64_COPYSIGN -> translateF64Copysign();
            case OP_I32_WRAP_I64 -> translateI32WrapI64();
            case OP_I32_TRUNC_F32_S -> translateI32TruncF32S();
            case OP_I32_TRUNC_F32_U -> translateI32TruncF32U();
            case OP_I32_TRUNC_F64_S -> translateI32TruncF64S();
            case OP_I32_TRUNC_F64_U -> translateI32TruncF64U();
            case OP_I64_EXTEND_I32_S -> translateI64ExtendI32S();
            case OP_I64_EXTEND_I32_U -> translateI64ExtendI32U();
            case OP_I64_TRUNC_F32_S -> translateI64TruncF32S();
            case OP_I64_TRUNC_F32_U -> translateI64TruncF32U();
            case OP_I64_TRUNC_F64_S -> translateI64TruncF64S();
            case OP_I64_TRUNC_F64_U -> translateI64TruncF64U();
            case OP_F32_CONVERT_I32_S -> translateF32ConvertI32S();
            case OP_F32_CONVERT_I32_U -> translateF32ConvertI32U();
            case OP_F32_CONVERT_I64_S -> translateF32ConvertI64S();
            case OP_F32_CONVERT_I64_U -> translateF32ConvertI64U();
            case OP_F32_DEMOTE_F64 -> translateF32DemoteF64();
            case OP_F64_CONVERT_I32_S -> translateF64ConvertI32S();
            case OP_F64_CONVERT_I32_U -> translateF64ConvertI32U();
            case OP_F64_CONVERT_I64_S -> translateF64ConvertI64S();
            case OP_F64_CONVERT_I64_U -> translateF64ConvertI64U();
            case OP_F64_PROMOTE_F32 -> translateF64PromoteF32();
            case OP_I32_REINTERPRET_F32 -> translateI32ReinterpretF32();
            case OP_I64_REINTERPRET_F64 -> translateI64ReinterpretF64();
            case OP_F32_REINTERPRET_I32 -> translateF32ReinterpretI32();
            case OP_F64_REINTERPRET_I64 -> translateF64ReinterpretI64();
            case OP_I32_EXTEND8_S -> translateI32Extend8S();
            case OP_I32_EXTEND16_S -> translateI32Extend16S();
            case OP_I64_EXTEND8_S -> translateI64Extend8S();
            case OP_I64_EXTEND16_S -> translateI64Extend16S();
            case OP_I64_EXTEND32_S -> translateI64Extend32S();
            case OP_REF_NULL -> translateRefNull();
            case OP_REF_IS_NULL -> translateRefIsNull();
            case OP_REF_FUNC -> translateRefFunc();
            case OP_CONT_PREFIX -> translateCont();
            default -> throw new TranslationException("Invalid opcode: 0x" + Integer.toHexString(Byte.toUnsignedInt(opcode)));
        }
    }

    private void translateUnreachable() {
        functionWriter.visitTypeInsn(NEW, UnreachableException.INTERNAL_NAME);
        functionWriter.visitInsn(DUP);
        functionWriter.visitMethodInsn(INVOKESPECIAL, UnreachableException.INTERNAL_NAME, "<init>", "()V", false);
        functionWriter.visitInsn(ATHROW);
        last(controlStack).markRestUnreachable();
    }

    private void translateNop() {
        // Nothing to do
    }

    private void translateBlock() throws TranslationException {
        var type = nextBlockType();
        checkTopOperands(type.parameterTypes());
        controlStack.add(new BlockScope(new Label(), type, operandStack.size()));
    }

    private void translateLoop() throws TranslationException {
        var startLabel = new Label();
        var type = nextBlockType();

        checkTopOperands(type.parameterTypes());
        controlStack.add(new LoopScope(startLabel, type, operandStack.size()));

        functionWriter.visitLabel(startLabel);
    }

    private void translateIf() throws TranslationException {
        popOperand(ValueType.I32);

        var elseLabel = new Label();
        var endLabel = new Label();
        var type = nextBlockType();

        checkTopOperands(type.parameterTypes());
        controlStack.add(new IfScope(elseLabel, endLabel, type, operandStack.size()));

        functionWriter.visitJumpInsn(IFEQ, elseLabel);
    }

    private @NotNull FunctionType nextBlockType() throws TranslationException {
        var code = reader.nextSigned33();
        if (code >= 0) {
            return parsedModule.types().get((int) code);
        }
        else {
            return switch ((byte) (code & 0x7F)) {
                case 0x40 -> new FunctionType(List.of(), List.of());
                case TYPE_I32 -> new FunctionType(List.of(), List.of(ValueType.I32));
                case TYPE_I64 -> new FunctionType(List.of(), List.of(ValueType.I64));
                case TYPE_F32 -> new FunctionType(List.of(), List.of(ValueType.F32));
                case TYPE_F64 -> new FunctionType(List.of(), List.of(ValueType.F64));
                case TYPE_EXTERNREF -> new FunctionType(List.of(), List.of(ValueType.EXTERNREF));
                case TYPE_FUNCREF -> new FunctionType(List.of(), List.of(ValueType.FUNCREF));
                default -> throw new TranslationException("Invalid block type");
            };
        }
    }

    private void translateElse() throws TranslationException {
        var scope = removeLast(controlStack);

        if (!(scope instanceof IfScope ifScope)) {
            throw new TranslationException("Expected IfScope on top of control stack");
        }

        if (!ifScope.restUnreachable()) {
            var actualReturnTypes = operandStack.subList(ifScope.baseOperandStackSize(), operandStack.size());
            if (!actualReturnTypes.equals(ifScope.type().returnTypes())) {
                throw new TranslationException("Return types mismatch at end of IfScope: expected " + ifScope.type().returnTypes() + ", found " + actualReturnTypes);
            }
        }

        operandStack.subList(ifScope.baseOperandStackSize(), operandStack.size()).clear();
        operandStack.addAll(ifScope.type().parameterTypes());
        controlStack.add(new BlockScope(ifScope.endLabel(), ifScope.type(), operandStack.size()));

        functionWriter.visitJumpInsn(GOTO, ifScope.endLabel());
        functionWriter.visitLabel(ifScope.elseLabel());
    }

    private void translateEnd() throws TranslationException {
        var scope = removeLast(controlStack);

        if (!scope.restUnreachable()) {
            var actualReturnTypes = operandStack.subList(scope.baseOperandStackSize(), operandStack.size());
            if (!actualReturnTypes.equals(scope.type().returnTypes())) {
                throw new TranslationException("Invalid stack at end of scope: expected " + scope.type().returnTypes() + " at top of stack, found " + actualReturnTypes);
            }
        }

        operandStack.subList(scope.baseOperandStackSize(), operandStack.size()).clear();
        operandStack.addAll(scope.type().returnTypes());

        if (scope instanceof IfScope ifScope) {
            functionWriter.visitLabel(ifScope.elseLabel());
            functionWriter.visitLabel(ifScope.endLabel());
        }
        else if (scope instanceof BlockScope blockScope) {
            functionWriter.visitLabel(blockScope.endLabel());
        }
    }

    private void translateBr() throws TranslationException {
        emitBranch(reader.nextUnsigned32());
        last(controlStack).markRestUnreachable();
    }

    private void translateBrIf() throws TranslationException {
        popOperand(ValueType.I32);

        var pastBranchLabel = new Label();
        functionWriter.visitJumpInsn(IFEQ, pastBranchLabel);
        emitBranch(reader.nextUnsigned32());
        functionWriter.visitLabel(pastBranchLabel);
    }

    private void translateBrTable() throws TranslationException {
        popOperand(ValueType.I32);

        var indexedTargetCount = reader.nextUnsigned32();
        var indexedTargets = new int[indexedTargetCount];

        for (var i = 0; i < indexedTargetCount; i++) {
            indexedTargets[i] = reader.nextUnsigned32();
        }

        var defaultTarget = reader.nextUnsigned32();

        var indexedAdapterLabels = new Label[indexedTargetCount];
        for (var i = 0; i < indexedTargetCount; i++) {
            indexedAdapterLabels[i] = new Label();
        }

        var defaultAdapterLabel = new Label();

        functionWriter.visitTableSwitchInsn(0, indexedTargetCount - 1, defaultAdapterLabel, indexedAdapterLabels);

        for (var i = 0; i < indexedTargetCount; i++) {
            functionWriter.visitLabel(indexedAdapterLabels[i]);
            emitBranch(indexedTargets[i]);
        }

        functionWriter.visitLabel(defaultAdapterLabel);
        emitBranch(defaultTarget);

        last(controlStack).markRestUnreachable();
    }

    private void translateReturn() throws TranslationException {
        var functionScope = (BlockScope) first(controlStack);
        checkTopOperands(functionScope.type().returnTypes());
        functionWriter.visitInsn(functionScope.type().returnOpcode());
        last(controlStack).markRestUnreachable();
    }

    private void translateCall() throws TranslationException {
        var index = reader.nextUnsigned32();
        var type = parsedModule.functionType(index);

        checkTopOperands(type.parameterTypes());
        removeLast(operandStack, type.parameterTypes().size());
        operandStack.addAll(type.returnTypes());

        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", type.descriptor(), ModuleImpl.FUNCTION_BOOTSTRAP, index);
    }

    private void translateCallIndirect() throws TranslationException {
        var type = parsedModule.types().get(reader.nextUnsigned32());

        popOperand(ValueType.I32);
        checkTopOperands(type.parameterTypes());
        removeLast(operandStack, type.parameterTypes().size());
        operandStack.addAll(type.returnTypes());

        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GET_METHOD_NAME, Table.GET_METHOD_DESCRIPTOR, false);
        functionWriter.visitTypeInsn(CHECKCAST, METHOD_HANDLE_INTERNAL_NAME);

        var calleeLocalIndex = firstScratchLocalIndex;
        functionWriter.visitVarInsn(ASTORE, calleeLocalIndex);

        var nextLocalIndex = calleeLocalIndex + 1;
        for (var i = type.parameterTypes().size() - 1; i >= 0; i--) {
            var parameterType = type.parameterTypes().get(i);
            functionWriter.visitVarInsn(parameterType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += parameterType.width();
        }

        functionWriter.visitVarInsn(ALOAD, calleeLocalIndex);

        for (var parameterType : type.parameterTypes()) {
            nextLocalIndex -= parameterType.width();
            functionWriter.visitVarInsn(parameterType.localLoadOpcode(), nextLocalIndex);
        }

        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", type.descriptor(), false);
    }

    private void translateDrop() throws TranslationException {
        functionWriter.visitInsn(popAnyOperand().isDoubleWidth() ? POP2 : POP);
    }

    private void translateSelect() throws TranslationException {
        popOperand(ValueType.I32);
        var type = popAnyOperand();
        checkTopOperand(type);

        var pastSwapLabel = new Label();
        functionWriter.visitJumpInsn(IFNE, pastSwapLabel);

        if (type.isDoubleWidth()) {
            functionWriter.visitInsn(DUP2_X2);
            functionWriter.visitInsn(POP2);
            functionWriter.visitLabel(pastSwapLabel);
            functionWriter.visitInsn(POP2);
        }
        else {
            functionWriter.visitInsn(SWAP);
            functionWriter.visitLabel(pastSwapLabel);
            functionWriter.visitInsn(POP);
        }
    }

    private void translateSelectVec() throws TranslationException {
        for (var i = reader.nextUnsigned32(); i != 0; i--) {
            reader.nextValueType();
        }

        translateSelect();
    }

    private void translateLocalGet() {
        var local = locals.get(reader.nextUnsigned32());
        operandStack.add(local.type());
        functionWriter.visitVarInsn(local.type().localLoadOpcode(), local.index());
    }

    private void translateLocalSet() throws TranslationException {
        var local = locals.get(reader.nextUnsigned32());
        popOperand(local.type());
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
    }

    private void translateLocalTee() throws TranslationException {
        var local = locals.get(reader.nextUnsigned32());
        applyUnaryOp(local.type());
        functionWriter.visitInsn(local.type().isDoubleWidth() ? DUP2 : DUP);
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
    }

    private void translateGlobalGet() {
        var globalIndex = reader.nextUnsigned32();
        var type = parsedModule.globalType(globalIndex).valueType();

        operandStack.add(type);

        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", type.globalGetDescriptor(), ModuleImpl.GLOBAL_GET_BOOTSTRAP, globalIndex);
    }

    private void translateGlobalSet() throws TranslationException {
        var globalIndex = reader.nextUnsigned32();
        var type = parsedModule.globalType(globalIndex).valueType();

        popOperand(type);

        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", type.globalSetDescriptor(), ModuleImpl.GLOBAL_SET_BOOTSTRAP, globalIndex);
    }

    private void translateTableGet() throws TranslationException {
        var index = reader.nextUnsigned32();

        applyUnaryOp(ValueType.I32, parsedModule.tableType(index).elementType());

        emitTableFieldLoad(index);
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GET_METHOD_NAME, Table.GET_METHOD_DESCRIPTOR, false);
    }

    private void translateTableSet() throws TranslationException {
        popReferenceOperand();
        popOperand(ValueType.I32);

        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.SET_METHOD_NAME, Table.SET_METHOD_DESCRIPTOR, false);
    }

    private void translateLoad(@NotNull ValueType resultType, @NotNull String name, @NotNull String descriptor) throws TranslationException {
        applyUnaryOp(ValueType.I32, resultType);

        reader.nextUnsigned32(); // expected alignment (ignored)
        pushI32Constant(functionWriter, reader.nextUnsigned32()); // offset

        emitMemoryFieldLoad(0);
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, name, descriptor, false);
    }

    private void translateI32Load() throws TranslationException {
        translateLoad(ValueType.I32, Memory.I32_LOAD_NAME, Memory.I32_LOAD_DESCRIPTOR);
    }

    private void translateI64Load() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_NAME, Memory.I64_LOAD_DESCRIPTOR);
    }

    private void translateF32Load() throws TranslationException {
        translateLoad(ValueType.F32, Memory.F32_LOAD_NAME, Memory.F32_LOAD_DESCRIPTOR);
    }

    private void translateF64Load() throws TranslationException {
        translateLoad(ValueType.F64, Memory.F64_LOAD_NAME, Memory.F64_LOAD_DESCRIPTOR);
    }

    private void translateI32Load8S() throws TranslationException {
        translateLoad(ValueType.I32, Memory.I32_LOAD_8_S_NAME, Memory.I32_LOAD_8_S_DESCRIPTOR);
    }

    private void translateI32Load8U() throws TranslationException {
        translateLoad(ValueType.I32, Memory.I32_LOAD_8_U_NAME, Memory.I32_LOAD_8_U_DESCRIPTOR);
    }

    private void translateI32Load16S() throws TranslationException {
        translateLoad(ValueType.I32, Memory.I32_LOAD_16_S_NAME, Memory.I32_LOAD_16_S_DESCRIPTOR);
    }

    private void translateI32Load16U() throws TranslationException {
        translateLoad(ValueType.I32, Memory.I32_LOAD_16_U_NAME, Memory.I32_LOAD_16_U_DESCRIPTOR);
    }

    private void translateI64Load8S() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_8_S_NAME, Memory.I64_LOAD_8_S_DESCRIPTOR);
    }

    private void translateI64Load8U() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_8_U_NAME, Memory.I64_LOAD_8_U_DESCRIPTOR);
    }

    private void translateI64Load16S() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_16_S_NAME, Memory.I64_LOAD_16_S_DESCRIPTOR);
    }

    private void translateI64Load16U() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_16_U_NAME, Memory.I64_LOAD_16_U_DESCRIPTOR);
    }

    private void translateI64Load32S() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_32_S_NAME, Memory.I64_LOAD_32_S_DESCRIPTOR);
    }

    private void translateI64Load32U() throws TranslationException {
        translateLoad(ValueType.I64, Memory.I64_LOAD_32_U_NAME, Memory.I64_LOAD_32_U_DESCRIPTOR);
    }

    private void translateStore(@NotNull ValueType operandType, @NotNull String name, @NotNull String descriptor) throws TranslationException {
        popOperand(operandType);
        popOperand(ValueType.I32);

        reader.nextUnsigned32(); // expected alignment (ignored)
        pushI32Constant(functionWriter, reader.nextUnsigned32()); // offset

        emitMemoryFieldLoad(0);
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, name, descriptor, false);
    }

    private void translateI32Store() throws TranslationException {
        translateStore(ValueType.I32, Memory.I32_STORE_NAME, Memory.I32_STORE_DESCRIPTOR);
    }

    private void translateI64Store() throws TranslationException {
        translateStore(ValueType.I64, Memory.I64_STORE_NAME, Memory.I64_STORE_DESCRIPTOR);
    }

    private void translateF32Store() throws TranslationException {
        translateStore(ValueType.F32, Memory.F32_STORE_NAME, Memory.F32_STORE_DESCRIPTOR);
    }

    private void translateF64Store() throws TranslationException {
        translateStore(ValueType.F64, Memory.F64_STORE_NAME, Memory.F64_STORE_DESCRIPTOR);
    }

    private void translateI32Store8() throws TranslationException {
        translateStore(ValueType.I32, Memory.I32_STORE_8_NAME, Memory.I32_STORE_8_DESCRIPTOR);
    }

    private void translateI32Store16() throws TranslationException {
        translateStore(ValueType.I32, Memory.I32_STORE_16_NAME, Memory.I32_STORE_16_DESCRIPTOR);
    }

    private void translateI64Store8() throws TranslationException {
        translateStore(ValueType.I64, Memory.I64_STORE_8_NAME, Memory.I64_STORE_8_DESCRIPTOR);
    }

    private void translateI64Store16() throws TranslationException {
        translateStore(ValueType.I64, Memory.I64_STORE_16_NAME, Memory.I64_STORE_16_DESCRIPTOR);
    }

    private void translateI64Store32() throws TranslationException {
        translateStore(ValueType.I64, Memory.I64_STORE_32_NAME, Memory.I64_STORE_32_DESCRIPTOR);
    }

    private void translateMemorySize() {
        operandStack.add(ValueType.I32);
        emitMemoryFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.SIZE_METHOD_NAME, Memory.SIZE_METHOD_DESCRIPTOR, false);
    }

    private void translateMemoryGrow() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        emitMemoryFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.GROW_METHOD_NAME, Memory.GROW_METHOD_DESCRIPTOR, false);
    }

    private void translateI32Const() {
        pushI32Constant(functionWriter, reader.nextSigned32());
        operandStack.add(ValueType.I32);
    }

    private void translateI64Const() {
        pushI64Constant(functionWriter, reader.nextSigned64());
        operandStack.add(ValueType.I64);
    }

    private void translateF32Const() {
        pushF32Constant(functionWriter, reader.nextFloat32());
        operandStack.add(ValueType.F32);
    }

    private void translateF64Const() {
        pushF64Constant(functionWriter, reader.nextFloat64());
        operandStack.add(ValueType.F64);
    }

    private void translateConditionalBoolean(int opcode) {
        var trueLabel = new Label();
        var mergeLabel = new Label();

        functionWriter.visitJumpInsn(opcode, trueLabel);
        functionWriter.visitInsn(ICONST_0);
        functionWriter.visitJumpInsn(GOTO, mergeLabel);

        functionWriter.visitLabel(trueLabel);
        functionWriter.visitInsn(ICONST_1);

        functionWriter.visitLabel(mergeLabel);
    }

    private void translateI32Eqz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        translateConditionalBoolean(IFEQ);
    }

    private void translateI32ComparisonS(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I32);
        translateConditionalBoolean(opcode);
    }

    private void translateI32ComparisonU(int opcode) throws TranslationException {
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "compareUnsigned", "(II)I", false);
        translateI32ComparisonS(opcode);
    }

    private void translateI32Eq() throws TranslationException {
        translateI32ComparisonS(IF_ICMPEQ);
    }

    private void translateI32Ne() throws TranslationException {
        translateI32ComparisonS(IF_ICMPNE);
    }

    private void translateI32LtS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPLT);
    }

    private void translateI32LtU() throws TranslationException {
        translateI32ComparisonU(IFLT);
    }

    private void translateI32GtS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPGT);
    }

    private void translateI32GtU() throws TranslationException {
        translateI32ComparisonU(IFGT);
    }

    private void translateI32LeS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPLE);
    }

    private void translateI32LeU() throws TranslationException {
        translateI32ComparisonU(IFLE);
    }

    private void translateI32GeS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPGE);
    }

    private void translateI32GeU() throws TranslationException {
        translateI32ComparisonU(IFGE);
    }

    private void translateI64Eqz() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(LCONST_0);
        functionWriter.visitInsn(LCMP);
        translateConditionalBoolean(IFEQ);
    }

    private void translateI64ComparisonS(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(LCMP);
        translateConditionalBoolean(opcode);
    }

    private void translateI64ComparisonU(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "compareUnsigned", "(JJ)I", false);
        translateConditionalBoolean(opcode);
    }

    private void translateI64Eq() throws TranslationException {
        translateI64ComparisonS(IFEQ);
    }

    private void translateI64Ne() throws TranslationException {
        translateI64ComparisonS(IFNE);
    }

    private void translateI64LtS() throws TranslationException {
        translateI64ComparisonS(IFLT);
    }

    private void translateI64LtU() throws TranslationException {
        translateI64ComparisonU(IFLT);
    }

    private void translateI64GtS() throws TranslationException {
        translateI64ComparisonS(IFGT);
    }

    private void translateI64GtU() throws TranslationException {
        translateI64ComparisonU(IFGT);
    }

    private void translateI64LeS() throws TranslationException {
        translateI64ComparisonS(IFLE);
    }

    private void translateI64LeU() throws TranslationException {
        translateI64ComparisonU(IFLE);
    }

    private void translateI64GeS() throws TranslationException {
        translateI64ComparisonS(IFGE);
    }

    private void translateI64GeU() throws TranslationException {
        translateI64ComparisonU(IFGE);
    }

    private void translateFpComparison(@NotNull ValueType type, int cmpOpcode, int branchOpcode) throws TranslationException {
        applyBinaryOp(type, ValueType.I32);
        functionWriter.visitInsn(cmpOpcode);
        translateConditionalBoolean(branchOpcode);
    }

    private void translateF32Eq() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFEQ);
    }

    private void translateF32Ne() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFNE);
    }

    private void translateF32Lt() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPG, IFLT);
    }

    private void translateF32Gt() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFGT);
    }

    private void translateF32Le() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPG, IFLE);
    }

    private void translateF32Ge() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFGE);
    }

    private void translateF64Eq() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFEQ);
    }

    private void translateF64Ne() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFNE);
    }

    private void translateF64Lt() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPG, IFLT);
    }

    private void translateF64Gt() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFGE);
    }

    private void translateF64Le() throws TranslationException {
        translateFpComparison(ValueType.F64, FCMPG, IFLE);
    }

    private void translateF64Ge() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFGE);
    }

    private void translateI32Clz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "numberOfLeadingZeros", "(I)I", false);
    }

    private void translateI32Ctz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "numberOfTrailingZeros", "(I)I", false);
    }

    private void translateI32Popcnt() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "bitCount", "(I)I", false);
    }

    private void translateI32Add() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IADD);
    }

    private void translateI32Sub() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISUB);
    }

    private void translateI32Mul() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IMUL);
    }

    private void translateI32DivS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_DIV_S_NAME, I32_DIV_S_DESCRIPTOR, false);
    }

    private void translateI32DivU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "divideUnsigned", "(II)I", false);
    }

    private void translateI32RemS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IREM);
    }

    private void translateI32RemU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "remainderUnsigned", "(II)I", false);
    }

    private void translateI32And() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IAND);
    }

    private void translateI32Or() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IOR);
    }

    private void translateI32Xor() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IXOR);
    }

    private void translateI32Shl() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISHL);
    }

    private void translateI32ShrS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISHR);
    }

    private void translateI32ShrU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IUSHR);
    }

    private void translateI32Rotl() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "rotateLeft", "(II)I", false);
    }

    private void translateI32Rotr() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "rotateRight", "(II)I", false);
    }

    private void translateI64Clz() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "numberOfLeadingZeros", "(J)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Ctz() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "numberOfTrailingZeros", "(J)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Popcnt() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "bitCount", "(J)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Add() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LADD);
    }

    private void translateI64Sub() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LSUB);
    }

    private void translateI64Mul() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LMUL);
    }

    private void translateI64DivS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_DIV_S_NAME, I64_DIV_S_DESCRIPTOR, false);
    }

    private void translateI64DivU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "divideUnsigned", "(JJ)J", false);
    }

    private void translateI64RemS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LREM);
    }

    private void translateI64RemU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "remainderUnsigned", "(JJ)J", false);
    }

    private void translateI64And() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LAND);
    }

    private void translateI64Or() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LOR);
    }

    private void translateI64Xor() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LXOR);
    }

    private void translateI64Shl() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(LSHL);
    }

    private void translateI64ShrS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(LSHR);
    }

    private void translateI64ShrU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(LUSHR);
    }

    private void translateI64Rotl() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "rotateLeft", "(JI)J", false);
    }

    private void translateI64Rotr() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "rotateRight", "(JI)J", false);
    }

    private void translateF32Abs() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "abs", "(F)F", false);
    }

    private void translateF32Neg() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(FNEG);
    }

    private void translateF32Ceil() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "ceil", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Floor() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "floor", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Trunc() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F32_TRUNC_NAME, F32_TRUNC_DESCRIPTOR, false);
    }

    private void translateF32Nearest() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "rint", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Sqrt() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "sqrt", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Add() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FADD);
    }

    private void translateF32Sub() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FSUB);
    }

    private void translateF32Mul() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FMUL);
    }

    private void translateF32Div() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FDIV);
    }

    private void translateF32Min() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "min", "(FF)F", false);
    }

    private void translateF32Max() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "max", "(FF)F", false);
    }

    private void translateF32Copysign() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "copySign", "(FF)F", false);
    }

    private void translateF64Abs() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "abs", "(D)D", false);
    }

    private void translateF64Neg() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitInsn(DNEG);
    }

    private void translateF64Ceil() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "ceil", "(D)D", false);
    }

    private void translateF64Floor() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "floor", "(D)D", false);
    }

    private void translateF64Trunc() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F64_TRUNC_NAME, F64_TRUNC_DESCRIPTOR, false);
    }

    private void translateF64Nearest() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "rint", "(D)D", false);
    }

    private void translateF64Sqrt() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "sqrt", "(D)D", false);
    }

    private void translateF64Add() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DADD);
    }

    private void translateF64Sub() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DSUB);
    }

    private void translateF64Mul() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DMUL);
    }

    private void translateF64Div() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DDIV);
    }

    private void translateF64Min() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "min", "(DD)D", false);
    }

    private void translateF64Max() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "max", "(DD)D", false);
    }

    private void translateF64Copysign() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "copySign", "(DD)D", false);
    }

    private void translateI32WrapI64() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(L2I);
    }

    private void translateI32TruncF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F32_S_NAME, I32_TRUNC_F32_S_DESCRIPTOR, false);
    }

    private void translateI32TruncF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F32_U_NAME, I32_TRUNC_F32_U_DESCRIPTOR, false);
    }

    private void translateI32TruncF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F64_S_NAME, I32_TRUNC_F64_S_DESCRIPTOR, false);
    }

    private void translateI32TruncF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F64_U_NAME, I32_TRUNC_F64_U_DESCRIPTOR, false);
    }

    private void translateI64ExtendI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.I64);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64ExtendI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
    }

    private void translateI64TruncF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F32_S_NAME, I64_TRUNC_F32_S_DESCRIPTOR, false);
    }

    private void translateI64TruncF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F32_U_NAME, I64_TRUNC_F32_U_DESCRIPTOR, false);
    }

    private void translateI64TruncF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F64_S_NAME, I64_TRUNC_F64_S_DESCRIPTOR, false);
    }

    private void translateI64TruncF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F64_U_NAME, I64_TRUNC_F64_U_DESCRIPTOR, false);
    }

    private void translateF32ConvertI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitInsn(I2F);
    }

    private void translateF32ConvertI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
        functionWriter.visitInsn(L2F);
    }

    private void translateF32ConvertI64S() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F32);
        functionWriter.visitInsn(L2F);
    }

    private void translateF32ConvertI64U() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F32_CONVERT_I64_U_NAME, F32_CONVERT_I64_U_DESCRIPTOR, false);
    }

    private void translateF32DemoteF64() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.F32);
        functionWriter.visitInsn(D2F);
    }

    private void translateF64ConvertI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F64);
        functionWriter.visitInsn(I2D);
    }

    private void translateF64ConvertI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
        functionWriter.visitInsn(L2D);
    }

    private void translateF64ConvertI64S() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitInsn(L2D);
    }

    private void translateF64ConvertI64U() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F64_CONVERT_I64_U_NAME, F64_CONVERT_I64_U_DESCRIPTOR, false);
    }

    private void translateF64PromoteF32() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.F64);
        functionWriter.visitInsn(F2D);
    }

    private void translateI32ReinterpretF32() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, FLOAT_INTERNAL_NAME, "floatToRawIntBits", "(F)I", false);
    }

    private void translateI64ReinterpretF64() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, DOUBLE_INTERNAL_NAME, "doubleToRawLongBits", "(D)J", false);
    }

    private void translateF32ReinterpretI32() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, FLOAT_INTERNAL_NAME, "intBitsToFloat", "(I)F", false);
    }

    private void translateF64ReinterpretI64() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, DOUBLE_INTERNAL_NAME, "longBitsToDouble", "(J)D", false);
    }

    private void translateI32Extend8S() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitInsn(I2B);
    }

    private void translateI32Extend16S() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitInsn(I2S);
    }

    private void translateI64Extend8S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2B);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Extend16S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2S);
        functionWriter.visitInsn(L2I);
    }

    private void translateI64Extend32S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2L);
    }

    private void translateRefNull() throws TranslationException {
        operandStack.add(reader.nextReferenceType());
        functionWriter.visitInsn(ACONST_NULL);
    }

    private void translateRefIsNull() throws TranslationException {
        popReferenceOperand();
        operandStack.add(ValueType.I32);
        translateConditionalBoolean(IFNULL);
    }

    private void translateRefFunc() {
        operandStack.add(ValueType.FUNCREF);
        var index = reader.nextUnsigned32();
        functionWriter.visitLdcInsn(new ConstantDynamic("_", METHOD_HANDLE_DESCRIPTOR, ModuleImpl.FUNCTION_REF_BOOTSTRAP, index));
    }

    private void translateCont() throws TranslationException {
        switch (reader.nextByte()) {
            case OP_CONT_I32_TRUNC_SAT_F32_S -> translateI32TruncSatF32S();
            case OP_CONT_I32_TRUNC_SAT_F32_U -> translateI32TruncSatF32U();
            case OP_CONT_I32_TRUNC_SAT_F64_S -> translateI32TruncSatF64S();
            case OP_CONT_I32_TRUNC_SAT_F64_U -> translateI32TruncSatF64U();
            case OP_CONT_I64_TRUNC_SAT_F32_S -> translateI64TruncSatF32S();
            case OP_CONT_I64_TRUNC_SAT_F32_U -> translateI64TruncSatF32U();
            case OP_CONT_I64_TRUNC_SAT_F64_S -> translateI64TruncSatF64S();
            case OP_CONT_I64_TRUNC_SAT_F64_U -> translateI64TruncSatF64U();
            case OP_CONT_MEMORY_INIT -> translateMemoryInit();
            case OP_CONT_DATA_DROP -> translateDataDrop();
            case OP_CONT_MEMORY_COPY -> translateMemoryCopy();
            case OP_CONT_MEMORY_FILL -> translateMemoryFill();
            case OP_CONT_TABLE_INIT -> translateTableInit();
            case OP_CONT_ELEM_DROP -> translateElemDrop();
            case OP_CONT_TABLE_COPY -> translateTableCopy();
            case OP_CONT_TABLE_GROW -> translateTableGrow();
            case OP_CONT_TABLE_SIZE -> translateTableSize();
            case OP_CONT_TABLE_FILL -> translateTableFill();
            default -> throw new TranslationException("Invalid opcode");
        }
    }

    private void translateI32TruncSatF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitInsn(F2I);
    }

    private void translateI32TruncSatF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_SAT_F32_U_NAME, I32_TRUNC_SAT_F32_U_DESCRIPTOR, false);
    }

    private void translateI32TruncSatF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitInsn(D2I);
    }

    private void translateI32TruncSatF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_SAT_F64_U_NAME, I32_TRUNC_SAT_F64_U_DESCRIPTOR, false);
    }

    private void translateI64TruncSatF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitInsn(F2L);
    }

    private void translateI64TruncSatF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_SAT_F32_U_NAME, I64_TRUNC_SAT_F32_U_DESCRIPTOR, false);
    }

    private void translateI64TruncSatF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitInsn(D2L);
    }

    private void translateI64TruncSatF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_SAT_F64_U_NAME, I64_TRUNC_SAT_F64_U_DESCRIPTOR, false);
    }

    private void translateMemoryInit() throws TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitDataFieldLoad(reader.nextUnsigned32());
        emitMemoryFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_METHOD_NAME, Memory.INIT_METHOD_DESCRIPTOR, false);
    }

    private void translateDataDrop() {
        // FIXME: make this do something
    }

    private void translateMemoryCopy() throws TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitMemoryFieldLoad(reader.nextUnsigned32());
        emitMemoryFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.COPY_METHOD_NAME, Memory.COPY_METHOD_DESCRIPTOR, false);
    }

    private void translateMemoryFill() throws TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitMemoryFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.FILL_METHOD_NAME, Memory.FILL_METHOD_DESCRIPTOR, false);
    }

    private void translateTableInit() throws TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitElementFieldLoad(reader.nextUnsigned32());
        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_METHOD_NAME, Table.INIT_METHOD_DESCRIPTOR, false);
    }

    private void translateElemDrop() {
        // FIXME: make this do something
    }

    private void translateTableCopy() throws TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitTableFieldLoad(reader.nextUnsigned32());
        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.COPY_METHOD_NAME, Table.COPY_METHOD_DESCRIPTOR, false);
    }

    private void translateTableGrow() throws TranslationException {
        popOperand(ValueType.I32);
        popReferenceOperand();
        operandStack.add(ValueType.I32);

        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GROW_METHOD_NAME, Table.GROW_METHOD_DESCRIPTOR, false);
    }

    private void translateTableSize() {
        operandStack.add(ValueType.I32);
        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.SIZE_METHOD_NAME, Table.SIZE_METHOD_DESCRIPTOR, false);
    }

    private void translateTableFill() throws TranslationException {
        popOperand(ValueType.I32);
        popReferenceOperand();
        popOperand(ValueType.I32);

        emitTableFieldLoad(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.FILL_METHOD_NAME, Table.FILL_METHOD_DESCRIPTOR, false);
    }

    private void checkHasOperand() throws TranslationException {
        if (operandStack.isEmpty() || operandStack.size() <= last(controlStack).baseOperandStackSize()) {
            throw new TranslationException("Operand stack underflow");
        }
    }

    private void checkTopOperand(@NotNull ValueType requiredType) throws TranslationException {
        checkHasOperand();
        if (last(operandStack) != requiredType) {
            throw new TranslationException("Wrong type at top of operand stack: expected " + requiredType + ", found " + last(operandStack));
        }
    }

    private void popOperand(@NotNull ValueType requiredType) throws TranslationException {
        checkTopOperand(requiredType);
        removeLast(operandStack);
    }

    private @NotNull ValueType popReferenceOperand() throws TranslationException {
        checkHasOperand();

        var last = last(operandStack);

        if (last != ValueType.EXTERNREF && last != ValueType.FUNCREF) {
            throw new TranslationException("Wrong type at top of operand stack: expected reference type, found " + last);
        }

        removeLast(operandStack);

        return last;
    }

    private @NotNull ValueType popAnyOperand() throws TranslationException {
        checkHasOperand();
        return removeLast(operandStack);
    }

    private void checkTopOperands(@NotNull List<ValueType> requiredTypes) throws TranslationException {
        var topTypes = operandStack.subList(Integer.max(operandStack.size() - requiredTypes.size(), last(controlStack).baseOperandStackSize()), operandStack.size());
        if (!topTypes.equals(requiredTypes)) {
            throw new TranslationException("Wrong operand types at top of stack: expected " + requiredTypes + ", found " + topTypes);
        }
    }

    private void applyUnaryOp(@NotNull ValueType inType, @NotNull ValueType outType) throws TranslationException {
        popOperand(inType);
        operandStack.add(outType);
    }

    private void applyUnaryOp(@NotNull ValueType type) throws TranslationException {
        checkTopOperand(type);
    }

    private void applyBinaryOp(@NotNull ValueType inType, @NotNull ValueType outType) throws TranslationException {
        popOperand(inType);
        popOperand(inType);
        operandStack.add(outType);
    }

    private void applyBinaryOp(@NotNull ValueType type) throws TranslationException {
        popOperand(type);
        checkTopOperand(type);
    }

    private void emitBranch(int index) throws TranslationException {
        // FIXME: stack fixup code isn't well-tested

        var scope = controlStack.get(controlStack.size() - index - 1);

        Label targetLabel;
        List<ValueType> labelParameterTypes;

        if (scope instanceof BlockScope blockScope) {
            targetLabel = blockScope.endLabel();
            labelParameterTypes = blockScope.type().returnTypes();
        }
        else if (scope instanceof IfScope ifScope) {
            targetLabel = ifScope.endLabel();
            labelParameterTypes = ifScope.type().returnTypes();
        }
        else if (scope instanceof LoopScope loopScope) {
            targetLabel = loopScope.startLabel();
            labelParameterTypes = loopScope.type().parameterTypes();
        }
        else {
            throw new ClassCastException();
        }

        checkTopOperands(labelParameterTypes);

        var nextLocalIndex = firstScratchLocalIndex;

        for (var i = labelParameterTypes.size() - 1; i >= 0; i--) {
            var parameterType = labelParameterTypes.get(i);
            functionWriter.visitVarInsn(parameterType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += parameterType.width();
        }

        for (var i = operandStack.size() - labelParameterTypes.size() - 1; i > scope.baseOperandStackSize(); i--) {
            functionWriter.visitInsn(operandStack.get(i).isDoubleWidth() ? POP2 : POP);
        }

        for (var parameterType : labelParameterTypes) {
            nextLocalIndex -= parameterType.width();
            functionWriter.visitVarInsn(parameterType.localLoadOpcode(), nextLocalIndex);
        }

        functionWriter.visitJumpInsn(GOTO, targetLabel);
    }

    private void emitDataFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", "(" + MODULE_INSTANCE_DESCRIPTOR + ")" + MEMORY_SEGMENT_DESCRIPTOR, ModuleImpl.DATA_FIELD_BOOTSTRAP, index);
    }

    private void emitElementFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", "(" + MODULE_INSTANCE_DESCRIPTOR + ")" + OBJECT_ARRAY_DESCRIPTOR, ModuleImpl.ELEMENT_FIELD_BOOTSTRAP, index);
    }

    private void emitMemoryFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", "(" + MODULE_INSTANCE_DESCRIPTOR + ")" + Memory.DESCRIPTOR, ModuleImpl.MEMORY_FIELD_BOOTSTRAP, index);
    }

    private void emitTableFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        functionWriter.visitInvokeDynamicInsn("_", "(" + MODULE_INSTANCE_DESCRIPTOR + ")" + Table.DESCRIPTOR, ModuleImpl.TABLE_FIELD_BOOTSTRAP, index);
    }
}

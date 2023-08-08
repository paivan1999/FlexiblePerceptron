package functionPackage


open class FuncNodeOrderComparator(type:String):Comparator<FuncNode> {
    open val baseForm:(FuncNode)->FuncNode = run {
        when (type) {
            "sum" -> { func -> baseFormForSum(func) }
            "prod" -> { func -> baseFormForProd(func) }
            else -> throw Exception("unexpected type of baseForm")
        }
    }
    fun compareFixedElementary(f1:FixedElementary,f2:FixedElementary):Int{
        return compareBy<FixedElementary> { f -> funcValueOrder(f) }.compare(f1, f2)
    }
    fun funcValueOrder(f:FixedElementary):Int = when (f){
        is Exp -> 0
        is Ln -> 1
        is Sin -> 2
        is ASin -> 3
        is Cos -> 4
        is ACos -> 5
        is Tan -> 6
        is ATan -> 7
        is Cosec -> 8
        is Sec -> 9
        is Cotan -> 10
        is BinaryPow -> 11
        is Prod -> 12
        is Sum -> 13
        else -> throw Exception()
    }
    fun isUnaryElementary(f:FixedElementary):Boolean{
        return funcValueOrder(f) <= 10
    }
    fun compareArgs(args1:Args<FuncNode>, args2:Args<FuncNode>):Int{
        if (args1.size != args2.size){
            return args1.size.compareTo(args2.size)
        } else {
            var result:Int
            for (i in 0 until args1.size){
                result = compareWithoutConstValue(args1[i],args2[i])
                if (result!=0){
                    return result
                }
            }
            return 0
        }
    }
    fun compareHigherFuncValues(o1: HigherFuncValue, o2: HigherFuncValue): Int {
        val e1 = o1.externElementary
        val e2 = o2.externElementary
        val result = compareFixedElementary(e1,e2)
        return if (result != 0) {
            result
        } else {
            compareArgs(o1.args,o2.args)
        }
    }
    fun baseFormForSum(fn:FuncNode):FuncNode{
        val o = fn.value
        if (o is HigherFuncValue){
            val args = o.args
            val e = o.externElementary
            if (e is Sum){
                throw UnpredictedError("baseFormForPlus must not work with sums")
            } else if (e is Prod) {
                if (args[0].value is ConstValue) {
                    if (args.size < 2) {
                        throw UnpredictedError("binary operator HigherFuncValue with less than two arguments")
                    }
                    return if (args.size == 2) {
                        args[1]
                    } else {
                        HigherFunc(e, args[IntRange(1, args.size)])
                    }
                } else {
                    return fn
                }
            } else {
                return fn
            }
        } else if (o is ConstValue){
            return One
        } else {
            return fn
        }
    }
    fun baseFormForProd(fn:FuncNode):FuncNode{
        val o = fn.value
        if (o is HigherFuncValue){
            val e = o.externElementary
            if (e is Prod){
                throw UnpredictedError("baseFormForProd must not work with prods")
            } else if (e is BinaryPow){
                val b1 = baseFormForSum(o.args[0])
                val b2 = baseFormForSum(o.args[1])
                if (b2.value is ConstValue) {
                    return b1
                } else {
                    return HigherFunc(e,b1,b2)
                }
            } else {
                return fn
            }
        } else if (o is ConstValue){
            return One
        } else {
            return fn
        }
    }
    fun getKForSum(fn:FuncNode):ConstValue{
        val o = fn.value
        if (o is ConstValue){
            return o
        } else if (o is VariableValue){
            return ConstValue(1.0)
        } else if (o is HigherFuncValue){
            val e = o.externElementary
            if (isUnaryElementary(e) || e is BinaryPow){
                return ConstValue(1.0)
            } else if (e is Sum){
                throw UnpredictedError("getKForSum must not work with Sums")
            } else if (e is Prod){
                val first = o.args[0].value
                if (first is ConstValue){
                    return first
                } else {
                    return ConstValue(1.0)
                }
            } else {
                throw UnpredictedError("Elementary must be Unary | BinaryPow | Sum | Prod")
            }
        } else {
            throw UnpredictedError("FuncValue must be ConstValue | VariableValue | HigherFuncValue")
        }
    }
    fun getKForProd(fn:FuncNode):Pair<ConstValue,ConstValue>{
        val o = fn.value
        if (o is HigherFuncValue){
            val e = o.externElementary
            if (isUnaryElementary(e) || e is Sum){
                return Pair(ConstValue(1.0),ConstValue(1.0))
            } else if (e is Prod){
                throw UnpredictedError("getKForProd must not work with Prods")
            } else if (e is BinaryPow){
                return Pair(getKForSum(o.args[0]),getKForSum(o.args[1]))
            } else {
                throw UnpredictedError("Elementary must be Unary | BinaryPow | Sum | Prod")
            }
        } else if (o is ConstValue){
            return Pair(o,ConstValue(1.0))
        } else if (o is VariableValue){
            return Pair(ConstValue(1.0),ConstValue(1.0))
        } else {
            throw UnpredictedError("FuncValue must be ConstValue | VariableValue | HigherFuncValue")
        }
    }
    override fun compare(o1: FuncNode, o2: FuncNode): Int {
        return compareWithoutConstValue(baseForm(o1), baseForm(o2))
    }
    fun compareWithoutConstValue(fn1: FuncNode, fn2: FuncNode): Int {
        val o1 = fn1.value
        val o2 = fn2.value
        return if (o1 is HigherFuncValue && o2 is HigherFuncValue) {
            compareHigherFuncValues(o1, o2)
        } else {
            compareBy<FuncValue>(
                { func -> func !is ConstValue },
                { func -> func !is VariableValue },
                { func ->
                    when (func) {
                        is ConstValue -> func.value
                        is VariableValue -> func.v.name
                        else -> throw UnpredictedError("must not go here (comparator)")
                    }
                }).compare(o1, o2)
        }
    }
}
package functionPackage

import kotlin.math.*
inline fun <reified T> typeName():String {
    return T::class.java.typeName
}

fun Const(value:Double):MixedFuncNode{
    return MixedFuncNode(ConstValue(value))
}
fun Variable(v:Var):MixedFuncNode{
    return MixedFuncNode(VariableValue(v))
}
fun HigherFunc(externElementary: FixedElementary,args:Args<FuncNode>):MixedFuncNode{
    return MixedFuncNode(MixedHigherFuncValue(externElementary,args))
}
fun HigherFunc(externElementary: FixedElementary,vararg args:FuncNode):MixedFuncNode{
    return MixedFuncNode(MixedHigherFuncValue(externElementary,*args))
}
val Zero = Const(0.0)
val One = Const(1.0)
val x: Var = Var("x")
val reservedNamedConstNames:Set<String> = setOf()
object SumComparator:FuncNodeOrderComparator("sum")
object ProdComparator:FuncNodeOrderComparator("prod")
fun wrapToUnFixedArgs(vararg args:FuncNode): UnFixedArgs<FuncNode> {
    return UnFixedArgs(mutableListOf(*args))
}
fun wrapToFixedArgs(vararg args:FuncNode): FixedArgs<FuncNode> {
    return FixedArgs(mutableListOf(*args))
}
fun wrapToFixed(nArgs:Int,argsToFunc:(Args<FuncNode>) -> FuncNode):(Args<FuncNode>)-> FuncNode {
    return {args -> run {
        if (args.size != nArgs) {
            throw IncorrectCountOfArgs("for FixedEvaluationRule")
        }
        argsToFunc(args)
    }}
}
fun anyToFullyMixedFuncValue(arg:Any): FuncValue {
    return when (arg) {
        is Var -> VariableValue(arg)
        is Double -> ConstValue(arg)
        is FuncValue -> if (arg is HigherFuncValue) arg.toFullyMixedHigherFuncValue() else arg
        is FuncNode -> throw Exception()
//        is FuncGraph -> arg.initialNode.value
        else -> throw IncorrectTypeOfArg()
    }
}
fun anyToFullyMixedFuncNode(arg:Any): MixedFuncNode {//TODO просмотри везде ли стоит клонировать узел
    return if (arg is FuncNode){
        arg.toFullyMixedFuncNode()
    } else {
        MixedFuncNode(anyToFullyMixedFuncValue(arg))
    }
}
fun anyToFuncValue(arg:Any):FuncValue{
    return when (arg) {
        is Var -> VariableValue(arg)
        is Double -> ConstValue(arg)
        is FuncValue -> arg
        is FuncNode -> throw Exception()
//        is FuncGraph -> arg.initialNode.value
        else -> throw IncorrectTypeOfArg()
    }
}
fun anyToFuncNode(arg:Any):FuncNode{
    return if (arg is FuncNode){
        arg
    } else {
        MixedFuncNode(anyToFuncValue(arg))
    }
}
fun allAreConst(args: Args<FuncNode>):Boolean = args.all{ func -> func.value is ConstValue }
fun allToDouble(args: Args<FuncNode>): Args<Double> = args.map{ arg -> (arg.value as ConstValue).value}
fun calcIfCan(externElementary: FixedElementary,values:Args<FuncNode>):FuncNode{
    if (allAreConst(values)){
        return Const(externElementary.doubleFunction(allToDouble(values)))
    }
    return HigherFunc(externElementary,values.toUnFixedArgs())
}
interface Arithmetic<T>{
    operator fun minus(other:Any):T
    operator fun plus(other:Any):T
    operator fun div(other:Any):T
    operator fun times(other:Any):T
    fun pow(other:Any):T
    operator fun unaryMinus():T
    fun inverse():T
    fun ln():T
    fun sin():T
    fun cos():T
    fun tan():T
    fun cotan():T
    fun sec():T
    fun cosec():T
    fun asin():T
    fun acos():T
    fun atan():T
    fun exp():T
    fun sqrt():T
}
//class FixingState(private var fixed : Boolean = false){
//    fun isFixed():Boolean{
//        return fixed
//    }
//    fun Fix(){
//        fixed = true
//    }
//    fun UnFix(){
//        fixed = false
//    }
//}
open class EvaluationRule(open val ruleFunction:(Args<FuncNode>) -> FuncNode){
    open operator fun invoke(args: Args<FuncNode>): FuncNode = ruleFunction(args)
    open operator fun invoke(vararg args: Any): FuncNode {
        return invoke(wrapToUnFixedArgs(*args.map { anyToFuncNode(it) }.toTypedArray()))
    }
    operator fun invoke(nArgs:Int):FixedEvaluationRule{
        return FixedEvaluationRule(nArgs,ruleFunction)
    }
}
open class FixedEvaluationRule(open val nArgs:Int, ruleFunction:(Args<FuncNode>)-> FuncNode): EvaluationRule(
    wrapToFixed(nArgs,ruleFunction)
){
    constructor(func: FuncNode):this(FixedFuncGraph(func).getVariablesSize(),{ args -> func(false,args)})
}

open class EvaluationRuleBlock(val block:(Args<FuncNode>)->Args<FuncNode>){
    fun apply(other:EvaluationRuleBlock):EvaluationRuleBlock{
        return EvaluationRuleBlock { args -> other.block(block(args)) }
    }
    fun finishToEvaluationRule(fixedElementaryBuilder:(Int)->FixedElementary):EvaluationRule{
        val result = EvaluationRule{args -> run {
            val resultArgs = block(args)
            if (resultArgs.size == 1) {
                resultArgs[0]
            } else {
                HigherFunc(fixedElementaryBuilder(resultArgs.size),resultArgs)
            }
        }}
        return result
    }
    fun finishUnary():FixedEvaluationRule{
        return FixedEvaluationRule(1){args ->
            block(args)[0]
        }
    }
    fun finishToFixedEvaluationRule(nArgs:Int, finalMapping: FixedElementary):FixedEvaluationRule{
        return FixedEvaluationRule(nArgs){args -> run {
            val resultArgs = block(args)
            if (resultArgs.size == 1) resultArgs[0] else HigherFunc(finalMapping,resultArgs)
        }}
    }
    fun finishToFixedEvaluationRule(
        nArgs:Int,
        fixedElementaryBuilder:(Int)->FixedElementary
    ):FixedEvaluationRule{
        return finishToEvaluationRule(fixedElementaryBuilder)(nArgs)
    }
}
object IdentityBlock:EvaluationRuleBlock({it})
open class SimpleRule(
    val elementary: Elementary
): EvaluationRule({ args -> HigherFunc(elementary(args.size),args) })
open class FixedSimpleRule(
    val fixedElementary: FixedElementary
): FixedEvaluationRule(fixedElementary.nArgs,{ args -> HigherFunc(fixedElementary,args) })

abstract class Mapping(open val evaluationRule: EvaluationRule?) {
    protected fun wrap(arg:EvaluationRule?):EvaluationRule{
        if (arg == null){
            throw UnpredictedError("it should not be null already")
        } else {
            return arg
        }
    }
    abstract fun asStr():String
    open operator fun invoke(args: Args<FuncNode>): FuncNode {
        return wrap(evaluationRule).invoke(args)
    }
    open operator fun invoke(vararg args: Any): FuncNode {
        return wrap(evaluationRule).invoke(wrapToUnFixedArgs(*args.map{anyToFuncNode(it)}.toTypedArray()))
    }
}
abstract class FixedMapping(val nArgs:Int, evaluationRule: FixedEvaluationRule?): Mapping(evaluationRule)
open class Elementary(
    val derivativeGenerator:(Args<Var>, Var)-> FuncNode,
    evaluationRule: EvaluationRule,//TODO maybe "?" not needed
    val doubleFunction: DoubleFunction,
    val name:String
): Mapping(evaluationRule){
    //    constructor(
//        evaluationRule: EvaluationRule?,
//        basisSimpleElementary: SimpleElementary
//    ):this(
//        basisSimpleElementary.derivativeGenerator,
//        evaluationRule,
//        basisSimpleElementary.doubleFunction,
//        basisSimpleElementary.name
//    )
    override fun asStr(): String {
        return name
    }
    open operator fun invoke(simplify: Boolean, args: Args<FuncNode>): FuncNode {
        return if (simplify){
            invoke(args)
        } else {
            calcIfCan(invoke(args.size),args)
        }
    }
    open operator fun invoke(simplify: Boolean,vararg args:Any):FuncNode{
        return invoke(simplify, wrapToUnFixedArgs(*args.map{ it -> anyToFuncNode(it) }.toTypedArray()))
    }
    open operator fun invoke(nArgs:Int): FixedElementary {
        return FixedElementary(
            nArgs,
            derivativeGenerator,
            FixedEvaluationRule(nArgs,wrap(evaluationRule).ruleFunction),
            doubleFunction,
            name
        )
    }
}

fun wrapFixedDerivativeGenerator(nArgs:Int, f:(Args<Var>, Var)-> FuncNode):(Args<Var>, Var)-> FuncNode {
    return {args,v -> run {
        if (args.size != nArgs){
            throw IncorrectCountOfArgs("for DerivativeGenerator")
        }
        if (args.toSet().contains(v)){
            f(args,v)
        } else {
            Zero
        }
    }}
}
open class FixedElementary(
    nArgs:Int,
    initialDerivativeGenerator:(Args<Var>, Var)-> FuncNode,
    evaluationRule: FixedEvaluationRule?,
    initialDoubleFunction: DoubleFunction,
    val name:String,
    alreadyTransformed:Boolean = false
): FixedMapping(nArgs,evaluationRule){
    override val evaluationRule: FixedEvaluationRule? = evaluationRule
    override fun asStr(): String {
        return name
    }
    val generatedVars = Var.generate(nArgs)
    fun grad(args:Args<FuncNode>,index:Int,simplify: Boolean):FuncNode{
        val result = FuncGraph(derivativeGenerator(generatedVars,generatedVars[index]))
        return result.evaluate(
            generatedVars.zip(args).toHashMap(),
            simplify,
            allToConsts = false,
            valuedParametersToConst = false,
            namedConstToConst = false,
            safeMode = false,
            argsAlreadyEvaluated = true
        )
    }
    val derivativeGenerator:(Args<Var>, Var)-> FuncNode
    val doubleFunction:DoubleFunction
    init {
        if (!alreadyTransformed) {
            derivativeGenerator = wrapFixedDerivativeGenerator(nArgs, initialDerivativeGenerator)
            doubleFunction = initialDoubleFunction.wrap(nArgs)
        } else {
            derivativeGenerator = initialDerivativeGenerator
            doubleFunction = initialDoubleFunction
        }
    }
    constructor(
        funcConst: FixedFuncGraph,
        evaluationRule: FixedEvaluationRule,
        name: String
    ):this(
        funcConst.getOrder().size,
        {args,v -> funcConst.grad(args,v)},
        evaluationRule,
        funcConst.evaluateEvaluable(),
        name
    )
    constructor(funcConst:FixedFuncGraph, name:String):this(
        funcConst.getOrder().size,
        {args,v -> funcConst.grad(args,v)},
        funcConst.evaluateEvaluationRule(),
        funcConst.evaluateEvaluable(),
        name
    )
    constructor(
        basisFixedElementary:FixedElementary
    ):this(
        basisFixedElementary.nArgs,
        basisFixedElementary.derivativeGenerator,
        basisFixedElementary.evaluationRule,
        basisFixedElementary.doubleFunction,
        basisFixedElementary.name,
        true
    )
    operator fun invoke(simplify: Boolean, args: Args<FuncNode>): FuncNode {
        return if (simplify){
            invoke(args)
        } else {
            calcIfCan(this,args)
        }
    }
    operator fun invoke(simplify: Boolean,vararg args: Any): FuncNode {
        return invoke(simplify, wrapToUnFixedArgs(*args.map{ it -> anyToFuncNode(it) }.toTypedArray()))
    }
}
open class FixedSimpleElementary: FixedElementary {
    constructor(
        nArgs:Int,
        derivativeGenerator:(Args<Var>, Var)-> FuncNode,
        doubleFunction: DoubleFunction,
        name:String,
    ):super(
        nArgs,
        derivativeGenerator,
        null,
        doubleFunction,
        name
    )
    constructor(func: FuncNode, name: String):super(FixedFuncGraph(func),name)
    constructor(
        func:FuncNode,
        evaluationRule: FixedEvaluationRule,
        name: String
    ):super(FixedFuncGraph(func),evaluationRule,name)

    override val evaluationRule by lazy {
        FixedSimpleRule(this)
    }
}

fun cleverPow(p1:FuncNode, p2:FuncNode):FuncNode{
    val v1 = p1.value
    val v2 = p2.value
    if (v1 is ConstValue){
        if (v1.value == 0.0) {
            return Zero
        }
        if (v1.value == 1.0) {
            return One
        }
    }
    if (v2 is ConstValue){
        if (v2.value == 0.0){
            return One
        }
        if (v2.value == 1.0){
            return p1
        }
    }
    if (v1 is ConstValue && v2 is ConstValue){
        return Const(v1.value.pow(v2.value))
    }
    return HigherFunc(BinaryPow,p1,p2)
}
fun cleverProd(args:Args<FuncNode>):FuncNode{
    if (allAreConst(args)){
        return Const(allToDouble(args).prod())
    }
    if (args.size == 1){
        return args[0]
    }
    val first = args[0].value
    if (first is ConstValue){
        if (first.value == 0.0){
            return Zero
        }
        if (first.value == 1.0){
            if (args.size == 2){
                return args[1]
            }
            return HigherFunc(Prod(args.size - 1),args[1..args.size])
        }
    }
    return HigherFunc(Prod(args.size),args)
}
fun similarPlus(similar:MutableList<FuncNode>):FuncNode{
    val baseForm = SumComparator.baseForm(similar[0])
    val baseFormValue = baseForm.value
    var k = 0.0
    for (s in similar){
        k+=SumComparator.getKForSum(s).value
    }
    return when(baseFormValue) {
        is ConstValue -> Const(k)
        is VariableValue -> cleverProd(UnFixedArgs(Const(k), baseForm))
        is HigherFuncValue ->
            if (baseFormValue.externElementary is Prod) {
                if (k == 1.0) baseForm else PolyProd(UnFixedArgs(Const(k))+baseFormValue.args)
            } else {
                cleverProd(UnFixedArgs(Const(k), baseForm))
            }
        else -> throw UnpredictedError("FuncValue must be ConstValue | VariableValue | HigherFuncValue")
    }
}

fun similarTimes(similar: MutableList<FuncNode>):FuncNode{
    val baseForm = ProdComparator.baseForm(similar[0])
    val baseFormValue = baseForm.value
    var k1 = 1.0
    var k2 = 0.0
    var p:Pair<ConstValue,ConstValue>
    for (s in similar){
        p = ProdComparator.getKForProd(s)
        k1 *= p.first.value.pow(p.second.value)
        k2 += p.second.value
    }
    val p1:FuncNode
    val p2:FuncNode
    if (baseFormValue is ConstValue){
        return Const(k1)
    } else if (baseFormValue is VariableValue){
        p1 = baseForm
        p2 = One
    } else if (baseFormValue is HigherFuncValue){
        val e = baseFormValue.externElementary
        if (ProdComparator.isUnaryElementary(e) || e is Sum){
            p1 = baseForm
            p2 = One
        } else if (e is Prod){
            throw UnpredictedError("Prod must not be here in SimilarProd")
        } else if (e is BinaryPow){
            p1 = baseFormValue.args[0]
            p2 = baseFormValue.args[1]
        } else {
            throw UnpredictedError("Extern Elementary can be only Unary | BinaryPow | Sum | Prod")
        }
    } else {
        throw UnpredictedError("FuncValue must be ConstValue | VariableValue | HigherFuncValue")
    }
    return cleverProd(UnFixedArgs(cleverPow(Const(k1),p2), cleverPow(p1, cleverProd(UnFixedArgs(Const(k2),p2)))))
}

open class SortBuilder(type:String):EvaluationRuleBlock(run {
    val funcIs:(FixedElementary)->Boolean
    val comparator:Comparator<FuncNode>
    when (type) {
        "sum" -> {
            funcIs = { it is Sum }
            comparator = SumComparator
        }
        "prod" -> {
            funcIs = { it is Prod }
            comparator = ProdComparator
        }
        else -> throw Exception("at the current moment type for builder can be only 'sum' and 'prod'")
    }
    { args -> run {
        val list = mutableListOf<Args<FuncNode>>()
        for (arg in args){
            val value = arg.value
            if (value is HigherFuncValue && funcIs(value.externElementary)){
                list.add(value.args)
            } else {
                list.add(UnFixedArgs(arg))
            }
        }
        mergeSorted(list,comparator)
    }}
})
object SortSum:SortBuilder("sum")
object SortProd:SortBuilder("prod")

open class SimilarBuilder(type:String):EvaluationRuleBlock(run {
    val similarCombine:(MutableList<FuncNode>)->FuncNode
    val comparator:FuncNodeOrderComparator
    val constIsNeutral:(Double)->Boolean
    when (type) {
        "sum" -> {
            comparator = SumComparator
            similarCombine = { similarPlus(it) }
            constIsNeutral = {it == 0.0}
        }
        "prod" -> {
            comparator = ProdComparator
            similarCombine = { similarTimes(it) }
            constIsNeutral = {it == 1.0}
        }
        else -> throw UnpredictedError("at the current moment there exist only two comparators: 'sum' and 'prod'")
    }
    { args -> run {
        val result = mutableListOf<FuncNode>()
        val stream = args.toStream()
        var similarList:MutableList<FuncNode>
        var current:FuncNode
        var baseForm:FuncNode
        var nextBaseForm:FuncNode
        var combined:FuncNode
        while (!stream.empty()){
            similarList = mutableListOf()
            baseForm = comparator.baseForm(stream.peek())
            nextBaseForm = baseForm
            while (nextBaseForm == baseForm){
                current = stream.next()
                similarList.add(current)
                if (stream.empty()){
                    break
                }
                nextBaseForm = comparator.baseForm(stream.peek())
            }
            combined = similarCombine(similarList)
            val value = combined.value
            if (!(value is ConstValue && constIsNeutral(value.value))) {
                result.add(combined)
            }
        }
        UnFixedArgs(result)
    }}
})
object SimilarSum:SimilarBuilder("sum")
object SimilarProd:SimilarBuilder("prod")


object PolySum: Elementary(
    { args, _ -> run {
        if (args.size == 0){
            throw ZeroArgsError("PolySum")
        }
        One
    }},
    SortSum.apply(SimilarSum).finishToEvaluationRule { nArgs -> Sum(nArgs) },
    DoubleFunction{it.sum()},
    "+"
){
    override fun invoke(nArgs: Int): FixedElementary = Sum(nArgs)
}
object PolyProd:Elementary(
    {args,v -> run {
        val innerArgs = args.removeByValue(v).toTypedArray()
        if (innerArgs.size == 1) Variable(innerArgs[0]) else Prod(innerArgs.size)(*innerArgs)
    }},
    SortProd.apply(SimilarProd).finishToEvaluationRule { nArgs -> Prod(nArgs) },
    DoubleFunction{it.prod()},
    "*"
){
    override fun invoke(nArgs: Int): FixedElementary = Prod(nArgs)
}
open class Sum(nArgs:Int):FixedElementary(
    nArgs,
    { args, _ -> run {
        if (args.size == 0){
            throw ZeroArgsError("PolySum")
        }
        One
    }},
    SortSum.apply(SimilarSum).finishToFixedEvaluationRule(nArgs){ count -> Sum(count) },
    DoubleFunction{it.sum()},
    "+"
){
    init {
        if (nArgs < 2){
            throw LessThanTwoArgsError("Sum")
        }
    }
}
open class Prod(nArgs:Int):FixedElementary(
    nArgs,
    {args,v -> run {
        val innerArgs = args.removeByValue(v).toTypedArray()
        if (innerArgs.size == 1) Variable(innerArgs[0]) else Prod(innerArgs.size)(*innerArgs)
    }},
    SortProd.apply(SimilarProd).finishToFixedEvaluationRule(nArgs) { count -> Prod(count) },
    DoubleFunction{it.prod()},
    "*"
) {
    init {
        if (nArgs < 2){
            throw LessThanTwoArgsError("Prod")
        }
    }
}

open class UnaryFunction: FixedElementary {
    constructor(
        derivative:(Var)-> FuncNode,
        evaluationRule: FixedEvaluationRule,
        evaluableFunc: (Double) -> Double,
        name:String
    ): super(
        1,
        {_,v -> derivative(v)},
        evaluationRule,
        DoubleFunction{evaluableFunc(it[0])},
        name
    )
    constructor(
        func: FuncNode,
        evaluationRule: FixedEvaluationRule,
        name: String
    ):super(FixedFuncGraph(func),evaluationRule,name)
    constructor(func:FuncNode,name:String):super(FixedFuncGraph(func),name)
}
open class UnarySimpleFunction(derivative:(Var)->FuncNode,doubleFunction:(Double)->Double,name:String):FixedSimpleElementary(
    1,
    {args,_ -> derivative(args[0])},
    DoubleFunction { args -> doubleFunction(args[0]) },
    name
)

open class BinaryFunction: FixedElementary {
    constructor(
        derivative1:(Var, Var)-> FuncNode,
        derivative2: (Var, Var)-> FuncNode,
        evaluationRule: FixedEvaluationRule,
        evaluableFunc: (Double, Double) -> Double,
        name:String
    ): super(
        2,
        {args:Args<Var>,v:Var -> if (v == args[0]) derivative1(args[0],args[1]) else derivative2(args[0],args[1])},
        evaluationRule,
        DoubleFunction{evaluableFunc(it[0],it[1])},
        name
    )
    constructor(
        func: FuncNode,
        evaluationRule: FixedEvaluationRule,
        name: String
    ):super(FixedFuncGraph(func),evaluationRule,name)

    constructor(func:FuncNode,name:String):super(FixedFuncGraph(func),name)
}
open class BinaryOperator: BinaryFunction {
    val priority:Int
    val leftAssociative:Boolean
    constructor(
        derivative1:(Var, Var)-> FuncNode,
        derivative2:(Var, Var)-> FuncNode,
        evaluationRule: FixedEvaluationRule,
        evaluableFunc: (Double, Double) -> Double,
        name:String,
        priority:Int,
        leftAssociative: Boolean
    ): super(derivative1,derivative2,evaluationRule,evaluableFunc,name){
        this.priority = priority
        this.leftAssociative = leftAssociative
    }
    constructor(
        func: FuncNode,
        evaluationRule: FixedEvaluationRule,
        name:String,
        priority: Int,
        leftAssociative: Boolean
    ):super(func,evaluationRule,name){
        this.priority = priority
        this.leftAssociative = leftAssociative
    }
    constructor(
        func:FuncNode,
        name:String,
        priority: Int,
        leftAssociative: Boolean
    ):super(func,name){
        this.priority = priority
        this.leftAssociative = leftAssociative
    }
}
object CleverPowBlock:EvaluationRuleBlock({args -> run {
    var pow1local = args[0]
    val pow2list = mutableListOf(args[1])
    while (pow1local.value is HigherFuncValue && (pow1local.value as HigherFuncValue).externElementary is BinaryPow){
        pow1local = (pow1local as HigherFuncValue).args[0]
        pow2list.add((pow1local as HigherFuncValue).args[1])
    }
    val pow1 = pow1local
    val pow2 = PolyProd(UnFixedArgs(pow2list))
    val value1 = pow1.value
    val value2 = pow2.value
    val pow1Args:Args<FuncNode> = if (value1 is HigherFuncValue) {
        if (value1.externElementary is Prod){
            value1.args
        } else {
            UnFixedArgs(pow1)
        }
    } else {
        UnFixedArgs(pow1)
    }
    val pow2Args:Args<FuncNode> = if (value2 is HigherFuncValue){
        if (value2.externElementary is Sum){
            value2.args
        } else {
            UnFixedArgs(pow2)
        }
    } else {
        UnFixedArgs(pow2)
    }
    val result = mutableListOf<FuncNode>()
    for (pow1Arg in pow1Args){
        for (pow2Arg in pow2Args){
            result.add(cleverPow(pow1Arg,pow2Arg))
        }
    }
    UnFixedArgs(result)
}})
//val q = SimilarProd.block(Args(Const(2.0)))
//val h = PolyProd(Args(Const(2.0)))
//val g = CleverPowBlock.block(Args(Const(2.0),Const(3.0)))
object BinaryPow:BinaryOperator(
    {v1,v2 -> v2 * (v1.pow(v2 - 1.0))},
    {v1,v2 ->v1.pow(v2)*v1.ln()},
    CleverPowBlock.finishToFixedEvaluationRule(2) { kArgs -> Prod(kArgs) },
    {x,y -> x.pow(y)},
    "^",
    3,
    false
)
object BinarySum:Sum(2)
object BinaryProd:Prod(2){
}
//val d = BinarySum
//{
//    val t = 0
//}

object CleverSinBlock:EvaluationRuleBlock({args -> run {
    val first = args[0]
    val value = first.value
    if (value is HigherFuncValue && value.externElementary == ASin){
        UnFixedArgs(value.args[0])
    } else {
        UnFixedArgs(HigherFunc(Sin,first))
    }
}})
object Sin:UnaryFunction(
    {x -> Cos(x)},
    CleverSinBlock.finishUnary(),
    {x -> sin(x)},
    "sin"
)

object CleverCosBlock : EvaluationRuleBlock({args -> run {
    val first = args[0]
    val value = first.value
    if (value is HigherFuncValue && value.externElementary == ACos){
        UnFixedArgs(value.args[0])
    } else {
        UnFixedArgs(HigherFunc(Cos,first))
    }
}})
object Cos:UnaryFunction(
    {x -> -Sin(x)},
    CleverCosBlock.finishUnary(),
    {x -> cos(x)},
    "cos"
)
object CleverTanBlock:EvaluationRuleBlock({args -> run {
    val first = args[0]
    val value = first.value
    if (value is HigherFuncValue && value.externElementary == ATan){
        UnFixedArgs(value.args[0])
    } else {
        UnFixedArgs(HigherFunc(Tan,first))
    }
}}) //TODO убрать лишние invoke
//val e = SortProd.apply(SimilarProd).block(Args())//.finishToFixedEvaluationRule(nArgs) { count -> Prod(count) }
val e = SortProd.apply(SimilarProd).block(UnFixedArgs(Cos.invoke(x),BinaryPow(Sin.invoke(x),Const(-1.0))))
val Sqrt = x.pow(0.5)
object Tan:UnaryFunction(Sin(x)/Cos(x),CleverTanBlock.finishUnary(),"tan")
object Cotan: UnaryFunction(Tan(x).inverse(),"cotan")
object Sec: UnaryFunction(Cos(x).inverse(),"sec")
object Cosec: UnaryFunction(Sin(x).inverse(),"cosec")
object ASin: UnarySimpleFunction({ v -> Sqrt(true,-v.pow(2.0)+1.0).inverse()},{ x -> asin(x) },"asin")
object ACos: UnarySimpleFunction({ v -> -Sqrt(true,-v.pow(2.0)+1.0).inverse()},{ x -> acos(x) },"acos")
object ATan: UnarySimpleFunction({ v -> (v.pow(2.0)+1.0).inverse()},{ x -> atan(x) },"atan")
object Exp: UnarySimpleFunction({ v -> Exp(v) },{ x -> exp(x) },"exp")
object Ln: UnarySimpleFunction({ v -> v.inverse()},{ x -> ln(x) },"ln")
class Var(val name:String): Arithmetic<FuncNode> {
    fun isVariable():Boolean{
        return name.startsWith("x")
    }
    fun isValuedParameter():Boolean{
        return name.startsWith("w")
    }
    fun isUnValuedParameter():Boolean{
        return !(isVariable() || isValuedParameter() || isNamedConst())
    }
    fun isNamedConst():Boolean{
        return reservedNamedConstNames.contains(name)
    }
    override fun equals(other: Any?): Boolean = other is Var && name == other.name
    companion object {
        fun generate(n: Int): Args<Var> {
            val generated = (1..n).map { Var("x$it") }.toMutableList()
            return UnFixedArgs(generated)
        }
    }
    override operator fun minus(other: Any): FuncNode = this + (-anyToFuncNode(other))
    override operator fun plus(other: Any): FuncNode = Variable(this) + other
    override operator fun div(other: Any): FuncNode = this * anyToFuncNode(other).inverse()
    override operator fun times(other: Any): FuncNode = Variable(this) * other
    override fun pow(other: Any): FuncNode = Variable(this).pow(other)
    override operator fun unaryMinus(): FuncNode = this * (-1.0)
    override fun inverse(): FuncNode = this.pow(-1.0)
    override fun sin(): FuncNode = Variable(this).sin()
    override fun cos(): FuncNode = Variable(this).cos()
    override fun tan(): FuncNode = Variable(this).tan()
    override fun cotan(): FuncNode = Variable(this).cotan()
    override fun sec(): FuncNode = Variable(this).sec()
    override fun cosec(): FuncNode = Variable(this).cosec()
    override fun asin(): FuncNode = Variable(this).asin()
    override fun acos(): FuncNode = Variable(this).acos()
    override fun atan(): FuncNode = Variable(this).atan()
    override fun ln(): FuncNode = Variable(this).ln()
    override fun exp(): FuncNode = Variable(this).exp()
    override fun sqrt(): FuncNode = Variable(this).sqrt()
    override fun hashCode(): Int = name.hashCode()
}
abstract class FuncEntity{
    protected var fixed : Boolean = false
    fun isFixed() : Boolean = fixed
}
interface FixedFuncEntity
interface FixedFuncValue:FixedFuncEntity{
    abstract fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ):FuncNode
}
interface MixedFuncEntity
interface MixedFuncValue:MixedFuncEntity
interface UnFixedFuncEntity
interface UnFixedFuncValue:UnFixedFuncEntity
abstract class FuncValue:FuncEntity(),Arithmetic<FuncValue>{
    open fun Fix(){
        fixed = true
    }
    abstract val differentiable:Boolean
    abstract val mutable:Boolean
    abstract val hash:Int
    abstract fun asStr():String
    abstract fun equalsByNames(other:FuncValue):Boolean
    abstract fun equalsByValues(other:FuncValue):Boolean
    abstract fun clone():FuncValue
    //    fun fullEvaluate(
//        hashMap: HashMap<Var, *>,
//        simplify:Boolean,
//        allToConsts:Boolean,
//        parametersToConst:Boolean,
//        namedConstToConst:Boolean
//    ):FuncNode{
//        return FuncNode(this).fullEvaluate(
//            hashMap,
//            simplify,
//            allToConsts,
//            parametersToConst,
//            namedConstToConst
//        )
//    }
    override operator fun minus(other: Any): FuncValue = this + (-anyToFuncValue(other))
    override operator fun div(other: Any): FuncValue = this * anyToFuncValue(other).inverse()
    override operator fun unaryMinus(): FuncValue = this * (-1.0)
    override fun inverse(): FuncValue = this.pow(-1.0)
    override operator fun plus(other: Any): FuncValue = BinarySum(true,this, other).value
    override operator fun times(other: Any): FuncValue = BinaryProd(true,this, other).value
    override fun pow(other: Any): FuncValue = BinaryPow(true,this,other).value
    override fun cos(): FuncValue = Cos(this).value
    override fun sin(): FuncValue = Sin(this).value
    override fun tan(): FuncValue = Tan(this).value
    override fun cotan(): FuncValue = Cotan(this).value
    override fun sec(): FuncValue = Sec(this).value
    override fun cosec(): FuncValue = Cosec(this).value
    override fun asin(): FuncValue = ASin(this).value
    override fun acos(): FuncValue = ACos(this).value
    override fun atan(): FuncValue = ATan(this).value
    override fun ln(): FuncValue = Ln(this).value
    override fun exp(): FuncValue = Exp(this).value
    override fun sqrt(): FuncValue = Sqrt(true,this).value
}
interface Doubled{
    val value:Double
}
abstract class HigherFuncValue(val externElementary: FixedElementary, open val args: Args<FuncNode>):FuncValue(){
    override val differentiable: Boolean = true
    override val hash: Int = 0 // TODO аналогичный вопрос
    override val mutable: Boolean = false
    constructor(externElementary: FixedElementary,vararg args:FuncNode):this(
        externElementary, wrapToUnFixedArgs(*args)
    )
    abstract fun toFixedHigherFuncValue():FixedHigherFuncValue
    abstract fun toUnFixedHigherFuncValue():UnFixedHigherFuncValue
    fun toFullyMixedHigherFuncValue():MixedHigherFuncValue{

    }
    override fun asStr(): String {
//        return externElementary.asStr()+"(${args.joinToString(",", FuncNode::asStr)})"
    }
    override fun equals(other: Any?): Boolean {
//        return other is HigherFuncValue &&
//                externElementary == other.externElementary &&
//                args == other.args
    }
    override fun equalsByNames(other: FuncValue):Boolean {
//        return other is HigherFuncValue &&
//                args.size == other.args.size &&
//                externElementary == other.externElementary &&
//                args.zip(other.args).all{it.first.equalsByNames(it.second)}
    }
    override fun equalsByValues(other: FuncValue): Boolean {
//        return other is FixedHigherFuncValue &&
//                args.size == other.args.size &&
//                externElementary == other.externElementary &&
//                args.zip(other.args).all{it.first.equalsByValues(it.second)}
    }
}
class FixedHigherFuncValue(externElementary: FixedElementary,override val args:FixedArgs<FixedFuncNode<*>>)
    :HigherFuncValue(externElementary,args),FixedFuncValue{
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return externElementary(simplify, args.map { node -> node.getEvaluated()!! })
    }
    fun grad(
        argIndex:Int,
        simplify: Boolean
    ):FuncNode {
        return externElementary.grad(args.map{node -> node.getEvaluated()!!},argIndex,simplify)
    }
    override fun clone(): FuncValue {
//        return MixedHigherFuncValue(externElementary,args)
    }
    override fun toFixedHigherFuncValue(): FixedHigherFuncValue {
        return this
    }
    override fun toUnFixedHigherFuncValue(): UnFixedHigherFuncValue {
//        return FixedHigherFuncValue(externElementary,args.map{it.toFixedFuncNode()}.toFixedArgs())
    }
}

class MixedHigherFuncValue(externElementary: FixedElementary, override val args:Args<FuncNode>)
    :HigherFuncValue(externElementary,args),MixedFuncValue{
    constructor(externElementary: FixedElementary,vararg args:FuncNode)
            :this(externElementary, wrapToUnFixedArgs(*args))
    override fun clone(): FuncValue {
//        return MixedHigherFuncValue(externElementary,args)
    }

    override fun toFixedHigherFuncValue(): FixedHigherFuncValue {
//        return FixedHigherFuncValue(externElementary,args.map{it.toFixedFuncNode()}.toFixedArgs())
    }

    override fun toUnFixedHigherFuncValue(): UnFixedHigherFuncValue {
//        return FixedHigherFuncValue(externElementary,args.map{it.toFixedFuncNode()}.toFixedArgs())
    }
}
class UnFixedHigherFuncValue(externElementary: FixedElementary,override val args:UnFixedArgs<UnFixedFuncNode<*>>)
    :HigherFuncValue(externElementary,args),UnFixedFuncValue{
    override fun clone(): FuncValue {
//        return MixedHigherFuncValue(externElementary,args)
    }

    override fun toFixedHigherFuncValue(): FixedHigherFuncValue {
//        return FixedHigherFuncValue(externElementary,args.map{it.toFixedFuncNode()}.toFixedArgs())
    }

    override fun toUnFixedHigherFuncValue(): UnFixedHigherFuncValue {
        return this
    }
}

abstract class WithoutChildren:FuncValue(),FixedFuncValue,MixedFuncValue,UnFixedFuncValue
abstract class Constant:WithoutChildren(),Doubled{
    override val differentiable: Boolean = false
    override val mutable: Boolean = false
    override val hash: Int = 0 // TODO is it needed?
    override fun equalsByValues(other: FuncValue): Boolean {
        return other is Doubled && value == other.value
    }
}
class ConstValue(override val value:Double):Constant(){
    override fun clone(): FuncValue = ConstValue(value)
    override fun asStr(): String = (if (value == value.roundToInt().toDouble()) value.roundToInt() else value).toString()
    constructor():this(0.0)
    override fun equalsByNames(other: FuncValue):Boolean {
        return other is ConstValue && value == other.value
    }
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return FixedFuncNode(this)
    }

    override fun equals(other: Any?): Boolean {
        return other is ConstValue && value == other.value
    }
}
class NamedConstValue(val v:Var, override val value:Double):Constant(),Doubled{
    override fun clone(): FuncValue = NamedConstValue(v,value)
    override fun asStr(): String {
        return v.name
    }
    override fun equalsByNames(other: FuncValue):Boolean {
        return other is NamedConstValue && v == other.v
    }
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return if (allToConsts || namedConstToConst) {
            Const(value)
        } else {
            FixedFuncNode(this)
        }
    }
    override fun equals(other: Any?): Boolean {
        return other is NamedConstValue && v == other.v && value == other.value
    }
}
abstract class Parameter(open val v:Var):WithoutChildren(){
    override val differentiable: Boolean = true
    override val hash: Int = 0 // TODO is needed?
    override fun asStr(): String {
        return v.name
    }
}
class ValuedParameter(v:Var, override var value:Double = 0.0):Parameter(v),Doubled{
    override val mutable: Boolean = true
    override fun clone(): FuncValue {
        return ValuedParameter(v,value)
    }
    override fun equalsByValues(other: FuncValue): Boolean {
        return other is Doubled && value == other.value
    }
    override fun equalsByNames(other: FuncValue):Boolean{
        return other is ValuedParameter && v == other.v
    }
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return if (allToConsts || parametersToConst){
            Const(value)
        } else {
            FixedFuncNode(this)
        }
    }
    override fun equals(other: Any?): Boolean {
        return other is ValuedParameter && v == other.v && value == other.value
    }
}
class UnValuedParameter(v:Var):Parameter(v){
    override val mutable: Boolean = false
    override fun clone(): FuncValue = UnValuedParameter(v)
    override fun equalsByNames(other: FuncValue):Boolean{
        return other is UnValuedParameter && v== other.v
    }
    override fun equalsByValues(other: FuncValue): Boolean {
        return equalsByNames(other)
    }
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return FixedFuncNode(this)
    }
    override fun equals(other: Any?): Boolean {
        return other is UnValuedParameter && v == other.v
    }
}
class VariableValue(val v:Var):WithoutChildren(){
    override val mutable: Boolean = false
    override val differentiable: Boolean = true
    override val hash: Int = 0 //TODO is needed?
    override fun clone(): FuncValue = VariableValue(v)
    override fun asStr(): String = v.name
    override fun equalsByNames(other: FuncValue):Boolean {
        return other is VariableValue && v == other.v
    }
    override fun equalsByValues(other: FuncValue): Boolean {
        return equalsByNames(other)
    }
    override fun evaluate(
        simplify: Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst: Boolean
    ): FuncNode {
        return FixedFuncNode(this)
    }
    override fun equals(other: Any?): Boolean {
        return other is VariableValue && v == other.v
    }
}
abstract class FuncNode(open val value: FuncValue):FuncEntity(),Arithmetic<FuncNode>{
    abstract val id:Int
    val idToIndexOfChildren:HashMap<Int,Int> = hashMapOf()
    abstract fun toFixedFuncNode():FixedFuncNode<*>
    abstract fun toUnFixedFuncNode():UnFixedFuncNode<*>
    fun toFullyMixedFuncNode():MixedFuncNode{

    }
    fun clone():FuncNode {

    }
//    fun <T: FuncNode> castTo(instance:T):T{
//        if (instance is FixedFuncNode<*>){
//            return toFixedFuncNode() as T
//        } else if (instance is UnFixedFuncNode){
//            return toUnFixedFuncNode() as T
//        } else {
//            throw Exception()
//        }
//    }
//    inline fun <reified T: FuncNode> cast():T{
//        if (typeName<T>() == "FuncNode"){
//            return this as T
//        } else if (typeName<T>() == "FixedFuncNode"){
//            if (this is FixedFuncNode<*>){
//                return this as T
//            } else {
//                return this.toFixedFuncNode() as T
//            }
//        } else if (typeName<T>() == "UnFixedFuncNode"){
//            if (this is FixedFuncNode<*>){
//                return this.toUnFixedFuncNode() as T
//            } else {
//                return this as T
//            }
//        } else {
//            throw Exception()
//        }
//    }
    init {
        if (value is FixedHigherFuncValue){
            var ind:Int = 0
            for (child in (value as FixedHigherFuncValue).args){
                idToIndexOfChildren[child.id] = ind
                ++ind
            }
        }
    }
    fun equalsByNames(other:FuncNode):Boolean{
//        return value.equalsByNames(other.value)
    }
    fun equalsByValues(other:FuncNode):Boolean{
//        return value.equalsByValues(other.value)
    }
    override fun equals(other: Any?): Boolean {
//        return other is FuncNode && value == other.value
    }
    operator fun invoke(simplify:Boolean,args:Args<FuncNode>):FuncNode{
        return FixedFuncGraph(this)(simplify,args)
    }
    operator fun invoke(simplify: Boolean,vararg args:Any):FuncNode{
        return FixedFuncGraph(this)(simplify,*args)
    }
    fun asStr():String{
//        return value.asStr()
    }
    fun idToChild(id:Int):FuncNode{
        if (value is FixedHigherFuncValue){
            return (value as FixedHigherFuncValue).args[idToIndexOfChildren[id]!!]
        } else if (value is MixedHigherFuncValue){
            return (value as MixedHigherFuncValue).args[idToIndexOfChildren[id]!!]
        } else {
            throw Exception()
        }
    }
    fun fullEvaluate(
        hashMap: HashMap<Var, *>,
        simplify:Boolean,
        allToConsts:Boolean,
        parametersToConst:Boolean,
        namedConstToConst:Boolean,
        safeMode: Boolean = true,
        argsAlreadyEvaluated: Boolean = false
    ):FuncNode {
        return FixedFuncGraph(this).evaluate(
            hashMap,
            simplify,
            allToConsts,
            parametersToConst,
            namedConstToConst,
            safeMode,
            argsAlreadyEvaluated
        )
    }
    fun getFuncGraphWithThisAsStart():FuncGraph{
        return FuncGraph(this)
    }
    override operator fun minus(other: Any): FuncNode = this + (-anyToFuncNode(other))
    override operator fun div(other: Any): FuncNode = this * anyToFuncNode(other).inverse()
    override operator fun unaryMinus(): FuncNode = this * (-1.0)
    override fun inverse(): FuncNode = this.pow(-1.0)
    override operator fun plus(other: Any): FuncNode = BinarySum(true,this,other)
    override operator fun times(other: Any): FuncNode = BinaryProd(true,this,other)
    override fun pow(other: Any): FuncNode = BinaryPow(true,this,other)
    override fun sin(): FuncNode = Sin(this)
    override fun cos(): FuncNode = Cos(this)
    override fun tan(): FuncNode = Tan(this)
    override fun cotan(): FuncNode = Cotan(this)
    override fun sec(): FuncNode = Sec(this)
    override fun cosec(): FuncNode = Cosec(this)
    override fun asin(): FuncNode = ASin(this)
    override fun acos(): FuncNode = ACos(this)
    override fun atan(): FuncNode = ATan(this)
    override fun ln(): FuncNode = Ln(this)
    override fun exp(): FuncNode = Exp(this)
    override fun sqrt(): FuncNode = Sqrt(false,this)

}
class FixedFuncNode<T>(override val value:T):FuncNode(value) where T : FuncValue, T:FixedFuncValue{
    val propogationState = State()
    override val id : Int
    override fun toFixedFuncNode(): FixedFuncNode<*> {
        return this
    }

    override fun toUnFixedFuncNode(): UnFixedFuncNode<*> {

    }
    init {
        id = newId()
        if (value is ConstValue){
            updateEvaluated(this)
        }
    }
    companion object {
        var current_id:Int = 0
        fun newId():Int{
            ++current_id
            return current_id
        }
    }
    fun evaluate(
        simplify:Boolean,
        allToConsts: Boolean,
        parametersToConst: Boolean,
        namedConstToConst:Boolean
    ){
        updateEvaluated(value.evaluate(simplify,allToConsts,parametersToConst,namedConstToConst))
    }
    fun grad(
        parents:MutableList<FixedFuncNode<*>>,
        simplify:Boolean
    ){
        val upperDerivatives = parents.map{ it.getGradByThis()!! }
        val derivativesByThis = parents.map{
            (it.value as HigherFuncValue).grad(it.idToIndexOfChildren[id]!!,simplify)
        }
        val prods = UnFixedArgs((upperDerivatives.zip(derivativesByThis).map{Prod(2)(simplify,it.first,it.second)}).toMutableList())
        updateGradByThis(PolySum(simplify,prods))
    }
    class State{
        var evaluated:FuncNode? = null
        var gradByThis:FuncNode? = null
        @JvmName("evaluated_getter")
        fun getEvaluated(): FuncNode? {
            return evaluated
        }
        fun updateEvaluated(new: FuncNode?) {
            evaluated = new
        }
        @JvmName("GradGetter")
        fun getGradByThis():FuncNode?{
            return gradByThis
        }
        fun updateGradByThis(new:FuncNode?){
            gradByThis = new
        }
    }
    fun getEvaluated():FuncNode?{
        return propogationState.getEvaluated()
    }
    fun updateEvaluated(new:FuncNode?){
        propogationState.updateEvaluated(new)
    }
    fun getGradByThis():FuncNode?{
        return propogationState.getGradByThis()
    }
    fun updateGradByThis(new:FuncNode?){
        propogationState.updateGradByThis(new)
    }
}
class MixedFuncNode(override var value: FuncValue):FuncNode(value){
    override val id : Int
    override fun toFixedFuncNode(): FixedFuncNode<*> {

    }
    override fun toUnFixedFuncNode(): UnFixedFuncNode<*> {

    }
    init {
        id = newId()
    }
    companion object {
        var current_id:Int = 0
        fun newId():Int{
            ++current_id
            return current_id
        }
    }
}


class UnFixedFuncNode<T>(override var value:T):FuncNode(value) where T : FuncValue, T:UnFixedFuncValue{
    override val id: Int
    override fun toFixedFuncNode(): FixedFuncNode<*> {

    }
    override fun toUnFixedFuncNode(): UnFixedFuncNode<*> {
        return this
    }
    init {
        id = newId()
    }
    companion object {
        var current_id:Int = 0
        fun newId():Int{
            ++current_id
            return current_id
        }
    }
    fun replaceThisByForOne(parent:FuncNode,other:FuncNode){
        val value = parent.value
        if (value !is HigherFuncValue){
            throw Exception()
        } else {
            val thisIndexByParent = parent.idToIndexOfChildren[id]!!
            parent.idToIndexOfChildren.remove(id)
            parent.idToIndexOfChildren[other.id] = thisIndexByParent
            value.args[thisIndexByParent] = other
        }
    }
    fun replaceThisBy(parents: MutableList<FuncNode>,other:FuncNode){ //IS NOT SAFE!
        for (parent in parents){
            replaceThisByForOne(parent,other)
        }
    }
}

class UnFixedTopologicalSorting(val sorting:ArrayDeque<UnFixedFuncNode<*>>){
    val reversedSorting = ArrayDeque(sorting.reversed())
    fun addAll(other:UnFixedTopologicalSorting){
        sorting.addAll(other.sorting)
        reversedSorting.addAll(other.reversedSorting)
    }
}

abstract class Order<T>(initializer:T){
    protected var order:T = initializer
    class NewOrderIsDifferentSetOfArgs:Exception("vars as set should be the same")
    protected abstract fun checkConsistency(newOrder:T):Boolean
    @JvmName("order_setter")
    fun setOrder(newOrder:T){
        if (!checkConsistency(newOrder)){
            throw NewOrderIsDifferentSetOfArgs()
        }
        order = newOrder
    }
    @JvmName("order_getter")
    fun getOrder():T{
        return order
    }
    abstract fun size():Int
}
class VariablesOrder(initialOrder:Args<Var>):Order<Args<Var>>(initialOrder){
    override fun checkConsistency(newOrder: Args<Var>): Boolean {
        return order.toSet() == newOrder.toSet()
    }
    override fun size(): Int = order.size
}
abstract class FuncGraph(open val firstNode:FuncNode):FuncEntity(),Arithmetic<FuncNode>{
    override fun minus(other: Any): FuncNode = firstNode - other
    override fun div(other: Any): FuncNode = firstNode / other
    override fun unaryMinus(): FuncNode = -firstNode
    override fun inverse(): FuncNode = firstNode.inverse()
    override operator fun plus(other: Any): FuncNode = firstNode + other
    override operator fun times(other: Any): FuncNode = firstNode * other
    override fun pow(other: Any): FuncNode = firstNode.pow(other)
    override fun sin(): FuncNode = firstNode.sin()
    override fun cos(): FuncNode = firstNode.cos()
    override fun tan(): FuncNode = firstNode.tan()
    override fun cotan(): FuncNode = firstNode.cotan()
    override fun sec(): FuncNode = firstNode.sec()
    override fun cosec(): FuncNode = firstNode.cosec()
    override fun asin(): FuncNode = firstNode.asin()
    override fun acos(): FuncNode = firstNode.acos()
    override fun atan(): FuncNode = firstNode.atan()
    override fun ln(): FuncNode = firstNode.ln()
    override fun exp(): FuncNode = firstNode.exp()
    override fun sqrt(): FuncNode = firstNode.sqrt()
}
class UnFixedFuncGraph(override val firstNode:UnFixedFuncNode<*>):FuncGraph(firstNode){
    constructor(initialValue:UnFixedHigherFuncValue):this(UnFixedFuncNode(initialValue))
    constructor(initialValue: WithoutChildren):this(UnFixedFuncNode(initialValue))
    val variables:HashMap<Var,UnFixedFuncNode<*>> = hashMapOf()
    val valuedParameters:HashMap<Var,UnFixedFuncNode<*>> = hashMapOf()
    val unvaluedParameters:HashMap<Var,UnFixedFuncNode<*>> = hashMapOf()
    val namedConsts:HashMap<Var,UnFixedFuncNode<*>> = hashMapOf()
    val topologicalSortingOfHigher:UnFixedTopologicalSorting
    init {
        val sorting = sortTopologically<UnFixedFuncNode<*>>(
            ArrayDeque(listOf(firstNode)),
            {node -> run {
                val value = node.value
                if (value is UnFixedHigherFuncValue){
                    value.args.toList()
                } else {
                    listOf()
                }
            }},
            {node -> node.id}
        )
        val highers:ArrayDeque<UnFixedFuncNode<*>> = ArrayDeque()
        var value:FuncValue
        for (node in sorting){
            value = node.value
            if (value is UnFixedHigherFuncValue) {
                highers.add(node)
                for (child in value.args){
                    replaceNamedEntity(child,node)
                }
            }
        }
        topologicalSortingOfHigher = UnFixedTopologicalSorting(highers)
    }
    private fun replaceNamedEntity(child:UnFixedFuncNode<*>,parent:UnFixedFuncNode<*>){
        val innerValue = child.value
        when (innerValue){
            is VariableValue ->
                if (variables.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,variables[innerValue.v]!!)
                } else {
                    variables[innerValue.v] = child
                }
            is NamedConstValue ->
                if (namedConsts.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,namedConsts[innerValue.v]!!)
                } else {
                    namedConsts[innerValue.v] = child
                }
            is ValuedParameter ->
                if (valuedParameters.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,valuedParameters[innerValue.v]!!)
                } else {
                    valuedParameters[innerValue.v] = child
                }
            is UnValuedParameter ->
                if (unvaluedParameters.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,unvaluedParameters[innerValue.v]!!)
                } else {
                    unvaluedParameters[innerValue.v] = child
                }
        }
    }
    fun getNotConstFinal():HashMap<Var,UnFixedFuncNode<*>>{
        return HashMap(variables + valuedParameters + namedConsts + unvaluedParameters)
    }
    fun getFinalForGrad():HashMap<Var,UnFixedFuncNode<*>>{
        return HashMap(variables + valuedParameters + unvaluedParameters)
    }
    private fun updateFinalNodesAccordingTo(newGraph:UnFixedFuncGraph) {
        var value: FuncValue
        for (node in newGraph.topologicalSortingOfHigher.sorting) {
            value = node.value
            if (value is UnFixedHigherFuncValue) {
                for (child in value.args) {
                    replaceNamedEntity(child, node)
                }
            } else {
                throw Exception()
            }
        }
    }
    fun subOneTransform(v:Var,substitute:UnFixedFuncNode<*>,safeMode: Boolean = true){
        if (fixed){
            throw Exception()
        }
        val value = substitute.value
        if (!v.isVariable()){
            throw Exception()
        }
        if (variables.containsKey(v)){
            val newValue = if (safeMode) value.clone() else value
            val vNode = variables.getValue(v)
            vNode.value = newValue
            val newGraph = FuncGraph(vNode)
            variables.remove(v)
            updateFinalNodesAccordingTo(newGraph)
            topologicalSorting.addAll(newGraph.topologicalSortingOfHigher)
        } else {
            throw Exception()
        }
    }
    fun subIndependentlyTransform(initialHashMap: HashMap<Var,*>,safeMode:Boolean = true){
        if (fixed){
            throw Exception()
        }
        val hashMap = initialHashMap.mapValues { anyToFuncValue(it.value) }
        for (v in hashMap.keys){
            if (!v.isVariable()){
                throw Exception()
            }
        }
        val newGraphs:ArrayDeque<FuncGraph> = ArrayDeque()
        for ((v,value) in hashMap){
            if (variables.containsKey(v)){
                val newValue = if (safeMode) value.clone() else value
                val vNode = variables.getValue(v)
                vNode.setValue(newValue)
                newGraphs.add(FuncGraph(vNode))
                variables.remove(v)
            } else {
                throw Exception()
            }
        }
        for (newGraph in newGraphs){
            updateFinalNodesAccordingTo(newGraph)
            topologicalSorting.addAll(newGraph.topologicalSortingOfHigher)
        }
    }
    //    fun simplifyByOrder(){
//
//    }
    fun simplifyByRules(
        allToConsts:Boolean,
        valuedParametersToConst:Boolean,
        namedConstToConst:Boolean
    ):FuncGraph{
        return FuncGraph(evaluate(
            hashMapOf<Var,Any>(),
            true,
            allToConsts,
            valuedParametersToConst,
            namedConstToConst
        ))
    }
    //    fun transform(v:Var,value:Double)
//    fun transform(v:Var)
    fun grad(
        simplify:Boolean
    ) {
        firstNode.updateGradByThis(One)
        val iter = topologicalSorting.sorting.iterator()
        iter.next()
        while(iter.hasNext()){
            iter.next().grad(simplify)
        }
        for (node in getFinalForGrad().values){
            node.grad(simplify)
        }
    }
    fun evaluate(
        initialHashMap: HashMap<Var, *>,
        simplify:Boolean,
        allToConsts:Boolean,
        valuedParametersToConst:Boolean,
        namedConstToConst:Boolean,
        safeMode: Boolean = true,
        argsAlreadyEvaluated:Boolean = false
    ):FuncNode{
        val hashMap = if (argsAlreadyEvaluated)
            initialHashMap.mapValues{if (safeMode) anyToMixedFuncNode(it).clone() else anyToMixedFuncNode(it)}
        else initialHashMap.mapValues {
            FuncNode(if (safeMode) anyToFuncValue(it.value).clone() else anyToFuncValue(it.value)).fullEvaluate(
                hashMapOf<Var,Any>(),
                simplify,
                allToConsts,
                valuedParametersToConst,
                namedConstToConst
            )}
        for (node in getNotConstFinal().values){
            node.evaluate(simplify,allToConsts, valuedParametersToConst,namedConstToConst)
        }
        for ((v,value) in hashMap){
            if (variables.containsKey(v)) {
                variables.getValue(v).updateEvaluated(value)
            }
        }
        for (node in topologicalSorting.reversedSorting){
            node.evaluate(simplify,allToConsts, valuedParametersToConst,namedConstToConst)
        }
        return firstNode.getEvaluated()!!
    }
}

class FixedFuncGraph(override val firstNode:FixedFuncNode<*>):FuncGraph(firstNode) {
    constructor(initialValue: FixedHigherFuncValue) : this(FixedFuncNode(initialValue))
    constructor(initialValue:WithoutChildren) : this(FixedFuncNode(initialValue))

    val variables:HashMap<Var,FuncNode> = hashMapOf()
    val valuedParameters:HashMap<Var,FuncNode> = hashMapOf()
    val unvaluedParameters:HashMap<Var,FuncNode> = hashMapOf()
    val namedConsts:HashMap<Var,FuncNode> = hashMapOf()
    val topologicalSortingOfHigher:UnFixedTopologicalSorting
    init {
        val sorting = sortTopologically<FuncNode>(
            ArrayDeque(listOf(firstNode)),
            {node -> run {
                val value = node.value
                if (value is HigherFuncValue){
                    value.args.toList()
                } else {
                    listOf()
                }
            }},
            {node -> node.id}
        )
        val highers:ArrayDeque<FuncNode> = ArrayDeque()
        var value:FuncValue
        var innerValue:FuncValue
        for (node in sorting){
            value = node.value
            if (value is HigherFuncValue) {
                highers.add(node)
                for (child in value.args){
                    replaceNamedEntity(child,node)
                }
            }
        }
        topologicalSortingOfHigher = UnFixedTopologicalSorting(highers)
    }
    private fun replaceNamedEntity(child:FuncNode,parent:FuncNode){
        val innerValue = child.value
        when (innerValue){
            is VariableValue ->
                if (variables.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,variables[innerValue.v]!!)
                } else {
                    variables[innerValue.v] = child
                }
            is NamedConstValue ->
                if (namedConsts.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,namedConsts[innerValue.v]!!)
                } else {
                    namedConsts[innerValue.v] = child
                }
            is ValuedParameter ->
                if (valuedParameters.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,valuedParameters[innerValue.v]!!)
                } else {
                    valuedParameters[innerValue.v] = child
                }
            is UnValuedParameter ->
                if (unvaluedParameters.containsKey(innerValue.v)) {
                    child.replaceThisByForOne(parent,unvaluedParameters[innerValue.v]!!)
                } else {
                    unvaluedParameters[innerValue.v] = child
                }
        }
    }
    fun getNotConstFinal():HashMap<Var,FuncNode>{
        return HashMap(variables + valuedParameters + namedConsts + unvaluedParameters)
    }
    fun getFinalForGrad():HashMap<Var,FuncNode>{
        return HashMap(variables + valuedParameters + unvaluedParameters)
    }
    private fun updateFinalNodesAccordingTo(newGraph:FuncGraph) {
        var value: FuncValue
        for (node in newGraph.topologicalSortingOfHigher.sorting) {
            value = node.value
            if (value is HigherFuncValue) {
                for (child in value.args) {
                    replaceNamedEntity(child, node)
                }
            } else {
                throw Exception()
            }
        }
    }
    fun subOneTransform(v:Var,substitute:FuncNode,safeMode: Boolean = true){
        if (fixed){
            throw Exception()
        }
        val value = substitute.value
        if (!v.isVariable()){
            throw Exception()
        }
        if (variables.containsKey(v)){
            val newValue = if (safeMode) value.clone() else value
            val vNode = variables.getValue(v)
            vNode.setValue(newValue)
            val newGraph = FuncGraph(vNode)
            variables.remove(v)
            updateFinalNodesAccordingTo(newGraph)
            topologicalSortingOfHigher.addAll(newGraph.topologicalSortingOfHigher)
        } else {
            throw Exception()
        }
    }
    fun subIndependentlyTransform(initialHashMap: HashMap<Var,*>,safeMode:Boolean = true){
        if (fixed){
            throw Exception()
        }
        val hashMap = initialHashMap.mapValues { anyToFuncValue(it.value) }
        for (v in hashMap.keys){
            if (!v.isVariable()){
                throw Exception()
            }
        }
        val newGraphs:ArrayDeque<FuncGraph> = ArrayDeque()
        for ((v,value) in hashMap){
            if (variables.containsKey(v)){
                val newValue = if (safeMode) value.clone() else value
                val vNode = variables.getValue(v)
                vNode.setValue(newValue)
                newGraphs.add(FuncGraph(vNode))
                variables.remove(v)
            } else {
                throw Exception()
            }
        }
        for (newGraph in newGraphs){
            updateFinalNodesAccordingTo(newGraph)
            topologicalSortingOfHigher.addAll(newGraph.topologicalSortingOfHigher)
        }
    }
    //    fun simplifyByOrder(){
//
//    }
    fun simplifyByRules(
        allToConsts:Boolean,
        valuedParametersToConst:Boolean,
        namedConstToConst:Boolean
    ):FuncGraph{
        return FuncGraph(evaluate(
            hashMapOf<Var,Any>(),
            true,
            allToConsts,
            valuedParametersToConst,
            namedConstToConst
        ))
    }
    //    fun transform(v:Var,value:Double)
//    fun transform(v:Var)
    fun grad(
        simplify:Boolean
    ) {
        firstNode.updateGradByThis(One)
        val iter = topologicalSortingOfHigher.sorting.iterator()
        iter.next()
        while(iter.hasNext()){
            iter.next().grad(simplify)
        }
        for (node in getFinalForGrad().values){
            node.grad(simplify)
        }
    }
    fun evaluate(
        initialHashMap: HashMap<Var, *>,
        simplify:Boolean,
        allToConsts:Boolean,
        valuedParametersToConst:Boolean,
        namedConstToConst:Boolean,
        safeMode: Boolean = true,
        argsAlreadyEvaluated:Boolean = false
    ):FuncNode{
        val hashMap = if (argsAlreadyEvaluated)
            initialHashMap.mapValues{if (safeMode) anyToMixedFuncNode(it).clone() else anyToMixedFuncNode(it)}
        else initialHashMap.mapValues {
            FuncNode(if (safeMode) anyToFuncValue(it.value).clone() else anyToFuncValue(it.value)).fullEvaluate(
                hashMapOf<Var,Any>(),
                simplify,
                allToConsts,
                valuedParametersToConst,
                namedConstToConst
            )}
        for (node in getNotConstFinal().values){
            node.evaluate(simplify,allToConsts, valuedParametersToConst,namedConstToConst)
        }
        for ((v,value) in hashMap){
            if (variables.containsKey(v)) {
                variables.getValue(v).updateEvaluated(value)
            }
        }
        for (node in topologicalSortingOfHigher.reversedSorting){
            node.evaluate(simplify,allToConsts, valuedParametersToConst,namedConstToConst)
        }
        return firstNode.getEvaluated()!!
    }
    private lateinit var variablesOrder:VariablesOrder
    private fun updateVariablesOrder(){
        variablesOrder = VariablesOrder(Args(funcGraph.variables.keys.sortedBy { v -> v.name }.toMutableList()))
    }
    fun setOrder(order:Args<Var>){
        variablesOrder.setOrder(order)
    }
    fun getOrder():Args<Var>{
        return variablesOrder.getOrder()
    }
    fun getVariablesSize():Int{
        return variablesOrder.size()
    }
    init {
        funcGraph.evaluate(
            hashMapOf<Var,Any>(),
            simplify = true,
            allToConsts = false,
            valuedParametersToConst = false,
            namedConstToConst = false
        )
        funcGraph.grad(true)
        updateVariablesOrder()
    }
    //    fun getEvaluated():FuncValue{
//        return funcGraph.initialNode.getEvaluated()!!
//    }
//    fun getHigherFuncValue():HigherFuncValue{
//        val value = initialNode.value
//        if (value !is HigherFuncValue){
//            throw Exception()
//        } else {
//            return value
//        }
//    }
    operator fun invoke(simplify:Boolean,args: Args<FuncNode>):FuncNode{
        return funcGraph.evaluate(
            getOrder().zip(args).toHashMap(),
            simplify,
            false,
            false,
            false
        )
    }
    operator fun invoke(simplify:Boolean,vararg args: Any):FuncNode{
        return funcGraph.evaluate(
            getOrder().zip(wrapToUnFixedArgs(*args)).toHashMap(),
            simplify,
            false,
            false,
            false
        )
    }
    fun evaluateEvaluationRule():FixedEvaluationRule{
        return FixedEvaluationRule(variablesOrder.size(),{args -> invoke(false,args)})
    }
    fun evaluateEvaluable():DoubleFunction{
        return DoubleFunction {args -> run {
            val result = invoke(false,args.map{Const(it)}).value
            if (result !is ConstValue){
                throw Exception()
            } else {
                result.value
            }
        }}
    }
    fun grad(args:Args<Var>,v:Var):FuncNode{
        val order = getOrder()
        if (args.size != order.size || !args.contains(v)) {
            throw Exception()
        }
        val index = args.indexOf(v)
        val result = funcGraph.variables[order[index]]!!.getEvaluated()!!
        return result.fullEvaluate(
            order.zip(args).toHashMap(),
            simplify = false,
            allToConsts = false,
            parametersToConst = false,
            namedConstToConst = false
        )
    }

}
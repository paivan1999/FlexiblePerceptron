package functionPackage

import java.util.PriorityQueue
import kotlin.collections.HashMap
import kotlin.math.*

class Stream<T>(l:Iterable<T>){
    private val list = l.toList()
    private var pos = 0
    fun empty():Boolean{
        return pos == list.size
    }
    fun next():T{
        val result = list[pos]
        pos += 1
        return result
    }
    fun peek():T{
        return list[pos]
    }
    fun reset(){
        pos = 0
    }

}

fun <K,V> Iterable<K>.toHashMap(transform:(K)->V): HashMap<K, V>{
    val result = HashMap<K,V>()
    for (el in this){
        result[el] = transform(el)
    }
    return result
}
fun <K,V> Iterable<Pair<K,V>>.toHashMap():HashMap<K,V>{
    val result = HashMap<K,V>()
    for ((first,second) in this){
        result[first] = second
    }
    return result
}
fun <T> mergeSorted(sorted:List<Args<T>>, comparator:Comparator<T>):Args<T>{
    val sortedIterators = sorted.map{it.toIterator()}
    val heap = PriorityQueue<Pair<Int,T>> { pair1, pair2 -> comparator.compare(pair1.second, pair2.second) }
    for ((i,iter) in sortedIterators.withIndex()){
        heap.add(Pair(i,iter.next()))
    }
    val result = mutableListOf<T>()
    var current:Pair<Int,T>
    var el:T
    var ind:Int
    var iter:Iterator<T>
    while (!heap.isEmpty()){
        current = heap.poll()
        el = current.second
        ind = current.first
        iter = sortedIterators[ind]
        result.add(el)
        if (iter.hasNext()){
            heap.add(Pair(ind,iter.next()))
        }
    }
    println(result)
    return UnFixedArgs(result)
}
//inline fun <reified T,reified D> callMethod(name:String):D{
////    println(T::class.java.getConstructor().)
//    try{
//        val method = T::class.java.getConstructor()
//        try {
//            val something = T::class.java.
//            if (something == null) {
//                println("null object instance")
//            }
//            val result = method.invoke(something)
//            try {
//                return result as D
//            } catch(e:Exception){
//                throw Exception("return value has incorrect type")
//            }
//        } catch(e:Exception){
//            throw Exception("${e.message}, can't instantiate class")
//        }
//    } catch(e:Exception){
//
//        throw Exception("${e.message}, there is no such method")
//    }
//}

//inline fun <reified T,reified D> get_field(name:String):D{
////    println(T::class.java.getConstructor().)
//    try{
//        val field = T::class.java.getField(name)
//        try {
//            val result = field.get(T::class.java.getConstructor().newInstance())
//            try {
//                return result as D
//            } catch (e: Exception) {
//                throw Exception("return value has incorrect type")
//            }
//        } catch(e:Exception){
//            throw Exception("can't instantiate class")
//        }
//    } catch(e:Exception){
//        throw Exception("there is no such field")
//    }
//}

//infix fun  <T, U, S, V> ((T, U, S) -> V).callWith(arguments: Triple<T, U, S>) : V = this(arguments.first, arguments.second, arguments.third)

class IncorrectTypeOfArg:Exception("all args for invoke should be Func | Var | Double")
class IncorrectCountOfArgs(s:String):Exception("incorrect count of arguments $s")
class ZeroArgsError(s:String):Exception("$s can't work with zero count of arguments")
class LessThanTwoArgsError(s:String):Exception("$s must take at least two arguments")




interface Summable<T>{
    operator fun plus(other:T):T
}
interface Multiplicable<T>{
    operator fun times(other:T):T
}



class DoubleFunction(val lambda:(Args<Double>)->Double){
    operator fun invoke(args:Args<Double>):Double{
        return lambda(args)
    }
    operator fun invoke(vararg args:Double):Double{
        return lambda(UnFixedArgs(args.toMutableList()))
    }
    fun wrap(nArgs:Int):DoubleFunction{
        return DoubleFunction{args -> run {
            if (args.size != nArgs){
                throw IncorrectCountOfArgs("for DoubleFunction")
            }
            this(args)
        }}
    }
}
abstract class Args<out T>(open val l:List<T>){
    val size = l.size
    fun toFixedArgs():FixedArgs<out T>{
        return if (this is FixedArgs<T>){
            this;
        } else {
            FixedArgs(l);
        }
    }
    fun toUnFixedArgs():UnFixedArgs<out T>{
        return if (this is UnFixedArgs<T>){
            this;
        } else {
            UnFixedArgs(l.toMutableList())
        }
    }
    override operator fun equals(other:Any?):Boolean{
        return other is Args<*> && l == other.l
    }
    override fun hashCode(): Int {
        return l.hashCode()
    }
    operator fun get(i:Int):T{
        return l[i]
    }
    operator fun get(rangeIndex:IntRange):UnFixedArgs<out T> {
        return UnFixedArgs(l.subList(rangeIndex.first, rangeIndex.last).toMutableList())
    }
    fun isEmpty():Boolean{
        return l.isEmpty()
    }
    fun toSet():Set<T>{
        return l.toSet()
    }
    fun toList():List<T>{
        return l.toList()
    }
    fun toStream():Stream<out T>{
        return Stream(l)
    }
    fun toIterator():Iterator<T>{
        return l.iterator()
    }
    operator fun iterator():Iterator<T>{
        return l.iterator()
    }
    open fun <R> map(transform: (T) -> R): UnFixedArgs<R> {
        return UnFixedArgs(l.map(transform).toMutableList())
    }
    fun all(predicate: (T) -> Boolean): Boolean{
        return l.all(predicate)
    }
    open fun <R,D> zip(other: Args<R>, zipFunction:(T, R)->D): UnFixedArgs<D> {
        val result = l.zip(other.l,transform = zipFunction).toMutableList()
        return UnFixedArgs(result)
    }
    fun joinToString(separator:String, transform:(T)->CharSequence):String{
        return l.joinToString(separator, transform = transform)
    }
}
fun <T> Args<T>.equals(other:Args<T>,comparator:(T,T)->Boolean):Boolean{
    return l.size == other.l.size && this.zip(other).all{pair -> comparator(pair.first,pair.second)}
}
operator fun <T> Args<T>.plus(other:Args<T>):UnFixedArgs<T>{
    return UnFixedArgs((l + other.l).toMutableList())
}

fun <T,R> Args<T>.zip(other: Args<R>): UnFixedArgs<Pair<T, R>> {
    return zip(other) { t, r -> Pair(t, r) }
}

fun <T> Args<T>.fold(initial: T,operation: (acc: T, T) -> T): T{
    var result = initial
    for (el in this){
        result = operation(result,el)
    }
    return result
}
fun <T> Args<T>.sortedWith(comparator:Comparator<T>):Args<T>{
    return if (this is FixedArgs<T>){
        FixedArgs(l.sortedWith(comparator))
    } else {
        UnFixedArgs(l.sortedWith(comparator).toMutableList())
    }
}
fun <T> Args<T>.removeByValue(value:T): List<T> {
    val result = l.toMutableList()
    result.removeAt(result.indexOf(value))
    return result
}
fun <T> Args<T>.indexOf(value:T):Int{
    return l.indexOf(value)
}
fun <T> Args<T>.contains(value:T):Boolean{
    return l.contains(value)
}
fun <K, V> Args<Pair<K, V>>.toHashMap(): HashMap<K, V> = l.toHashMap()
inline fun <reified T> Args<T>.sum():T where T : Summable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result += l[i]
    }
    return result
}
inline fun <reified T> Args<T>.prod():T where T : Multiplicable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result *= l[i]
    }
    return result
}
fun Args<Double>.sum():Double = fold(0.0){ acc, el -> acc + el}
fun Args<Double>.prod():Double = fold(1.0){ acc, el -> acc * el}
inline fun <reified T> Args<T>.scalarDot(other: Args<T>):T where T: Summable<T>, T: Multiplicable<T> {
    return zip(other) { t, r -> t * r }.sum()
}
class FixedArgs<out T>(override val l:List<T>):Args<T>(l){
    constructor(vararg args:T):this(args.toList())
    fun <R,D> zip(other: FixedArgs<R>, zipFunction:(T,R)->D): FixedArgs<D> {
        val result = l.zip(other.l,transform = zipFunction).toMutableList()
        return FixedArgs(result)
    }
    fun <R> zip(other: FixedArgs<R>): FixedArgs<Pair<T,R>> {
        return zip(other) { t, r -> Pair(t, r) }
    }

}

fun <T> FixedArgs<T>.equals(other:Args<T>,comparator:(T,T)->Boolean):Boolean{
    return l.size == other.l.size && this.zip(other).all{pair -> comparator(pair.first,pair.second)}
}
operator fun <T> FixedArgs<T>.plus(other:Args<T>):UnFixedArgs<T>{
    return UnFixedArgs((l + other.l).toMutableList())
}
operator fun <T> FixedArgs<T>.plus(other:FixedArgs<T>):FixedArgs<T>{
    return FixedArgs(l + other.l)
}

fun <T,R> FixedArgs<T>.zip(other: Args<R>): UnFixedArgs<Pair<T, R>> {
    return zip(other) { t, r -> Pair(t, r) }
}

fun <T> FixedArgs<T>.fold(initial: T,operation: (acc: T, T) -> T): T{
    var result = initial
    for (el in this){
        result = operation(result,el)
    }
    return result
}
fun <T> FixedArgs<T>.sortedWith(comparator:Comparator<T>):FixedArgs<T>{
    return FixedArgs(l.sortedWith(comparator))
}
fun <T> FixedArgs<T>.removeByValue(value:T): List<T> {
    val result = l.toMutableList()
    result.removeAt(result.indexOf(value))
    return result
}
fun <T> FixedArgs<T>.indexOf(value:T):Int{
    return l.indexOf(value)
}
fun <T> FixedArgs<T>.contains(value:T):Boolean{
    return l.contains(value)
}
fun <K, V> FixedArgs<Pair<K, V>>.toHashMap(): HashMap<K, V> = l.toHashMap()
inline fun <reified T> FixedArgs<T>.sum():T where T : Summable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result += l[i]
    }
    return result
}
inline fun <reified T> FixedArgs<T>.prod():T where T : Multiplicable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result *= l[i]
    }
    return result
}
fun FixedArgs<Double>.sum():Double = fold(0.0){ acc, el -> acc + el}
fun FixedArgs<Double>.prod():Double = fold(1.0){ acc, el -> acc * el}
inline fun <reified T> FixedArgs<T>.scalarDot(other: FixedArgs<T>):T where T: Summable<T>, T: Multiplicable<T> {
    return zip(other) { t, r -> t * r }.sum()
}

class UnFixedArgs<T>(override var l:MutableList<T>):Args<T>(l){
    constructor(vararg args:T):this(args.toMutableList())
    operator fun set(i:Int,value:T){
        l[i] = value
    }
    fun equals(other:Args<T>,comparator:(T,T)->Boolean):Boolean{
        return l.size == other.l.size && this.zip(other).all{pair -> comparator(pair.first,pair.second)}
    }
    operator fun plus(other:Args<T>):UnFixedArgs<T>{
        return UnFixedArgs((l + other.l).toMutableList())
    }

    fun <R> zip(other: Args<R>): UnFixedArgs<Pair<T, R>> {
        return zip(other) { t, r -> Pair(t, r) }
    }

    fun fold(initial: T,operation: (acc: T, T) -> T): T{
        var result = initial
        for (el in this){
            result = operation(result,el)
        }
        return result
    }
    fun sortedWith(comparator:Comparator<T>):Args<T>{
        return UnFixedArgs(l.sortedWith(comparator).toMutableList())
    }
    fun removeByValue(value:T): List<T> {
        val result = l.toMutableList()
        result.removeAt(result.indexOf(value))
        return result
    }
    fun indexOf(value:T):Int{
        return l.indexOf(value)
    }
    fun contains(value:T):Boolean{
        return l.contains(value)
    }
}
fun <K, V> UnFixedArgs<Pair<K, V>>.toHashMap(): HashMap<K, V> = l.toHashMap()
inline fun <reified T> UnFixedArgs<T>.sum():T where T : Summable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result += l[i]
    }
    return result
}
inline fun <reified T> UnFixedArgs<T>.prod():T where T : Multiplicable<T> {
    if (this.isEmpty()){
        throw UnpredictedError("list should not be empty")
    }
    var result = l[0]
    for (i in 1 until l.size){
        result *= l[i]
    }
    return result
}
fun UnFixedArgs<Double>.sum():Double = fold(0.0){ acc, el -> acc + el}
fun UnFixedArgs<Double>.prod():Double = fold(1.0){ acc, el -> acc * el}
inline fun <reified T> UnFixedArgs<T>.scalarDot(other: UnFixedArgs<T>):T where T: Summable<T>, T: Multiplicable<T> {
    return zip(other) { t, r -> t * r }.sum()
}
//fun WrapUnaryDouble(f:(Double)->Double):(Args<Double>)->Double{
//    return {arg -> arg.let{
//        if (it.size != 1){
//            throw Exception("Unary function should get one Double")
//        }
//        f(it[0])
//    }}
//}
//fun WrapBinaryDouble(f:(Double,Double)->Double):(Args<Double>)->Double{
//    return {arg -> arg.let{
//        if (it.size != 2){
//            throw Exception("Unary function should get two Double")
//        }
//        f(it[0],it[1])
//    }}
//}
//fun WrapUnaryFunc(f:(Var)->Func):(Args<Var>,Var)->Func{
//    return {args,v -> run{
//        if (args.size != 1){
//            throw Exception("Unary function should get one Var")
//        }
//        if (args[0] == v){
//            f(v)
//        } else {
//            Const()
//        }
//    }}
//}
//fun WrapBinaryFunc(derivative1:(Var,Var)->Func,derivative2: (Var,Var)->Func):(Args<Var>,Var)->Func{
//    return {args,v -> run{
//        if (args.size != 2){
//            throw Exception("Binary function should get two Vars")
//        }
//        if (args[0] == v){
//            derivative1(args[0],args[1])
//        } else if (args[1] == v){
//            derivative2(args[0],args[1])
//        } else{
//            Const()
//        }
//    }}
//}

//object CleverExpBlock:EvaluationRuleBlock({args -> run {
//    val first = args[0]
//    if (first is HigherFunc && first.externElementary == ATan){
//        Args(first.args[0])
//    } else {
//        Args(HigherFunc(Tan,first))
//    }
//}})


//object SimpleSqrt: UnarySimpleFunction({ v -> SimpleSqrt(v).inverse()*0.5},{ x -> sqrt(x)},"sqrt")

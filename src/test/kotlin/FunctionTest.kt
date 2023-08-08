//package functionPackage
//
//import kotlin.math.pow
//import org.junit.jupiter.api.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotEquals
//
//class FunctionTest{
//    private val one = Const(1.0)
//    private val negone = Const(-1.0)
//    private val x = Var("x")
//    private val y = Var("y")
//    private val x1 = Var("x")
//    private val vars = Var.generate(3)
//    private val f = Variable(x).pow(y)
//    private val h =
//                Variable(x).pow(Variable(x))*
//                Variable(x).pow(Variable(y))*
//                Variable(y).pow(Variable(x))
//    private val g =h * Variable(y).pow(Variable(y)).pow(Variable(y))
//    @Test
//    fun testMergeSorted() {
//        val args1 = Args(1, 4, 7, 10)
//        val args2 = Args(2, 3, 5)
//        val args3 = Args(6, 8, 9)
//        val g = (1..10).toList()
//        assertEquals(Args((1..10).toMutableList()), mergeSorted(listOf(args1, args2, args3), Comparator { a, b -> a - b }))
//        val args4 = Args(0)
//        val args5 = Args(1)
//        val args6 = Args(10)
//        val args7 = Args((1..10).toMutableList())
//        assertEquals(
//            Args(0,1,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,10),
//            mergeSorted(listOf(args1,args2,args3,args4,args5,args6,args7), Comparator{ a, b -> a - b})
//        )
//    }
//    @Test
//    fun testVar(){
//        assert(x==x1)
//        assert(x!=y)
//        assertEquals(vars,Args(Var("x1"),Var("x2"),Var("x3")))
//
//        assertEquals(x+y,Variable(x)+Variable(y))
//        assertEquals(x-y,Variable(x)-Variable(y))
//        assertEquals(x*y,Variable(x)*Variable(y))
//        assertEquals(x/y,Variable(x)/Variable(y))
//        assertEquals(x.pow(y),Variable(x).pow(Variable(y)))
//
//        assertEquals(x+2.0,Variable(x)+Const(2.0))
//        assertEquals(x-2.0,Variable(x)-Const(2.0))
//        assertEquals(x*2.0,Variable(x)*Const(2.0))
//        assertEquals(x/2.0,Variable(x)/Const(2.0))
//        assertEquals(x.pow(2.0),Variable(x).pow(Const(2.0)))
//
//        assertEquals(x+f,Variable(x)+f)
//        assertEquals(x-f,Variable(x)-f)
//        assertEquals(x*f,Variable(x)*f)
//        assertEquals(x/f,Variable(x)/f)
//        assertEquals(x.pow(f),Variable(x).pow(f))
//
//        assertEquals(-x,Variable(x)*(negone))
//        assertEquals(x.inverse(),Variable(x).pow(negone))
//        assertEquals(x.sin(),Variable(x).sin())
//        assertEquals(x.cos(),Variable(x).cos())
//        assertEquals(x.tan(),Variable(x).tan())
//        assertEquals(x.cotan(),Variable(x).cotan())
//        assertEquals(x.sec(),Variable(x).sec())
//        assertEquals(x.cosec(),Variable(x).cosec())
//        assertEquals(x.asin(),Variable(x).asin())
//        assertEquals(x.acos(),Variable(x).acos())
//        assertEquals(x.atan(),Variable(x).atan())
//        assertEquals(x.ln(),Variable(x).ln())
//        assertEquals(x.exp(),Variable(x).exp())
//        assertEquals(x.sqrt(),Variable(x).sqrt())
//    }
//    @Test
//    fun testArgs(){
//        assertEquals(vars,Args(Var("x1"),Var("x2"),Var("x3")))
//        assertEquals(Args(),Args<Var>())
//        assertNotEquals(Args(),Args(x))
//        assertNotEquals(Args(x),Args(y))
//        assertEquals(Args(x),Args(x1))
//        assertEquals(Var("x2"),vars[1])
//        vars[2]=Var("x4")
//        assertEquals(Var("x4"),vars[2])
//        assertEquals(setOf(Var("x1"),Var("x4"),Var("x2")),vars.toSet())
//        assertEquals(listOf(Var("x1"),Var("x2"),Var("x4")),vars.toList())
//        for ((v,i) in vars.zip(Args(1,2,4))){
//            assertEquals(v,Var("x$i"))
//        }
//        assertEquals(vars.map{it.name[1]},Args('1','2','4'))
//        assert(vars.all{it.name.length==2})
//        assert(!vars.all{it.name[0]=='y'})
//        assertEquals(vars.zip(Args(1,2,3)),Args(Pair(Var("x1"),1),Pair(Var("x2"),2),Pair(Var("x4"),3)))
//        assertEquals(vars.joinToString(" ") { v -> "${v.name}!" },"x1! x2! x4!")
//        assertEquals(Args(Var("x")).joinToString(" ") { v -> "${v.name}!" },"x!")
//        assertEquals(Args(1.0,2.0,3.0).fold(2.0){x,y->x.pow(y)},64.0)
//        assertEquals(Args<Double>().fold(2.0){x,y->x.pow(y)},2.0)
//        assertEquals(vars.removeByValue(Var("x2")), listOf(Var("x1"),Var("x4")))
//        assertEquals(vars.removeByValue(Var("x1")), listOf(Var("x2"),Var("x4")))
//        assertEquals(vars.removeByValue(Var("x4")), listOf(Var("x1"),Var("x2")))
//        assertEquals(Args(Var("x1")).removeByValue(Var("x1")), listOf())
//        assertEquals(Args(Pair(1.0,2.0)).toHashMap(), hashMapOf(1.0 to 2.0))
//        class F(val n:Double):Summable<F>,Multiplicable<F>{
//            override fun plus(other: F): F = F(n+other.n)
//            override fun times(other: F): F = F(n * other.n)
//        }
//        val vars1 = Args(F(1.0),F(2.0),F(3.0))
//        val vars2 = Args(F(2.0),F(3.0),F(4.0))
//        assertEquals(vars1.sum().n,6.0)
//        assertEquals(vars2.prod().n,24.0)
//        assertEquals(vars1.scalarDot(vars2).n,20.0)
//    }
//    @Test
//    fun testFunc(){
//        //TEST UNARY AND BINARY OPERATORS
//        val This = one+Variable(x)
//        val Other = This - Const(2.9)
//        assertEquals(This + Other,SimpleBinarySum(This,Other))
//        assertEquals(This-Other,SimpleBinarySum(This,SimpleBinaryProd(Other,negone)))
//        assertEquals(This*Other,SimpleBinaryProd(This,Other))
//        assertEquals(This/Other,SimpleBinaryProd(This,SimpleBinaryPow(Other,negone)))
//        assertEquals(This.pow(Other),SimpleBinaryPow(This,Other))
//        assertEquals(This+x,SimpleBinarySum(This,Variable(x)))
//        assertEquals(This-x,SimpleBinarySum(This,SimpleBinaryProd(x,negone)))
//        assertEquals(This*x,SimpleBinaryProd(This,Variable(x)))
//        assertEquals(This/x,SimpleBinaryProd(This,SimpleBinaryPow(Variable(x),negone)))
//        assertEquals(This.pow(x),SimpleBinaryPow(This,Variable(x)))
//        assertEquals(f+1.0,SimpleBinarySum(f,one))
////        assertEquals(one+one,Const(2.0))
//        val q = f-1.0
//        assertEquals(f-1.0,SimpleBinarySum(f,negone))
//        assertEquals(f*1.0,SimpleBinaryProd(f,one))
//        assertEquals(f/1.0,SimpleBinaryProd(f,one))
//        assertEquals(f.pow(1.0),SimpleBinaryPow(f,one))
//        assertEquals(f.sin(),SimpleSin(f))
//        assertEquals(f.cos(),SimpleCos(f))
//        assertEquals(f.tan(),SimpleTan(f))
//        assertEquals(f.cotan(),SimpleCotan(f))
//        assertEquals(f.sec(),SimpleSec(f))
//        assertEquals(f.cosec(),SimpleCosec(f))
//        assertEquals(f.asin(),SimpleASin(f))
//        assertEquals(f.acos(),SimpleACos(f))
//        assertEquals(f.atan(),SimpleATan(f))
//        assertEquals(f.exp(),SimpleExp(f))
//        assertEquals(f.ln(),SimpleLn(f))
//        assertEquals(f.sqrt(),SimpleSqrt(f))
//        assertEquals(f.inverse(),SimpleBinaryPow(f,negone))
//        assertEquals(f.unaryMinus(),SimpleBinaryProd(f,negone))
//        //TEST sub
//        assertEquals(f.sub(hashMapOf(x to y,y to x)),f.sub(hashMapOf(x to Variable(y),y to Variable(x))))
//        assertEquals(f.sub(hashMapOf(x to 2.0,y to 3.0)),f.sub(hashMapOf(x to Const(2.0),y to Const(3.0))))
//        assertEquals(one.sub(hashMapOf(x to one)),one)
//        assertEquals(Variable(x).sub(hashMapOf(x to one)),one)
//        assertEquals(Variable(x).sub(hashMapOf(y to one)),Variable(x))
//        //TEST evaluateEvaluationRule
//        assertEquals(g.evaluateEvaluationRule()(3.0,2.0),Const(3.0.pow(5.0)*(2.0.pow(7.0))))
//        assertEquals(g.evaluateEvaluationRule()(y,2.0),h.evaluateEvaluationRule()(y,2.0)*Const(16.0))
//        assertEquals(Variable(x).evaluateEvaluationRule()(1.0),one)
//        assertEquals(Variable(x).evaluateEvaluationRule()(x),Variable(x))
//        assertEquals(one.evaluateEvaluationRule()(),one)
//        //TEST evaluateEvaluable
//        assertEquals(g.evaluateEvaluable()(3.0,2.0),3.0.pow(5.0)*(2.0.pow(7.0)))
//        assertEquals(g.evaluateEvaluationRule()(y,2.0).evaluateEvaluable()(3.0),3.0.pow(5.0)*(2.0.pow(7.0)))
//        assertEquals(Variable(x).evaluateEvaluable()(2.0),2.0)
//        assertEquals(one.evaluateEvaluable()(),1.0)
//        //TEST setOrder getOrder
//        val g1 = g.clone()
//        g1.setVariableOrder(y,x)
//        assertEquals(g1.getOrder(),Args(y,x))
//        assertEquals(g1.evaluateEvaluable()(2.0,3.0),3.0.pow(5.0)*(2.0.pow(7.0)))
//        val v = Variable("v")
//        v.setVariableOrder(Var("v"))
//        assertEquals(v.getOrder(),Args(Var("v")))
//        val c = Const(1.0)
//        c.setVariableOrder()
//        assertEquals(c.getOrder(),Args())
//        //TEST invoke
//        assertEquals(f(f,f),f.pow(f))
//        assertEquals(v(1.0),one)
//        assertEquals(c(),one)
//        assertEquals(f(y,one),y.pow(one))
//        assertEquals(f(hashMapOf(y to 1.0)),x.pow(one))
//        assertEquals(one(hashMapOf(y to 1.0)),one)
//        assertEquals(Variable(x)(hashMapOf(y to 1.0)),Variable(x))
//        assertEquals(Variable(x)(hashMapOf(x to 1.0)),one)
//    }
//    @Test
//    fun testHigherFunc(){
//        //TEST equals and clone
//        assert(g == g.clone())
////        assert(one == one)
////        assert(Variable(x)==Variable(x))
////        assert(one != negone)
////        assert(Variable(y) != Variable(x))
//        //TEST constructors
//        assertEquals(HigherFunc(SimpleBinaryPow,x,y),HigherFunc(SimpleBinaryPow,Args(Variable(x),Variable(y))))
//        //TEST sub
//        assertEquals(f.sub(hashMapOf(x to f)),f.pow(y))
//        //TEST evaluate
//        assertEquals(HigherFunc(SimpleBinaryPow,2.0,3.0).evaluate(),Const(8.0))
//        //TEST getVarsAsArgs
//        assertEquals((x+x+x).getVarsAsArgs(),Args(x))
//        assertEquals((x+x+y).getVarsAsArgs(),Args(x,y))
//        assertEquals((x+y+x).getVarsAsArgs(),Args(x,y))
//        assertEquals((y+x+x).getVarsAsArgs(),Args(y,x))
//        //TEST grad
//            //TEST for derivative by one argument
//                //TEST for the same Var
//        assertEquals(SimpleSin(x).grad(x),SimpleCos(x)*one)
//        assertEquals(SimpleCos(x).grad(x),-SimpleSin(x)*one)
//        assertEquals(SimpleASin(x).grad(x),SimpleSqrt(-x.pow(2.0)+1.0).inverse()*one)
//        assertEquals(SimpleACos(x).grad(x),-SimpleSqrt(-x.pow(2.0)+1.0).inverse()*one)
//        assertEquals(SimpleATan(x).grad(x),(x.pow(2.0)+1.0).inverse()*one)
//        assertEquals(SimpleExp(x).grad(x),SimpleExp(x)*one)
//        assertEquals(SimpleLn(x).grad(x),x.inverse()*one)
//        assertEquals((x+x).grad(x),Const(2.0))
//        assertEquals((x+x).grad(x),Const(2.0))
//        val q = (x*x).grad(x)
//        assertEquals((x*x).grad(x),x*one+x*one)
//        assertEquals(x.pow(x).grad(x),x*x.pow(x-1.0)*one+x.pow(x)*SimpleLn(x)*one)
//        assertEquals(SimpleSin(x).pow(2.0).grad(x),Const(2.0)*SimpleSin(x).pow(one)*SimpleSin(x).grad(x)+
//                SimpleSin(x).pow(2.0)*SimpleLn(SimpleSin(x))*Const())
//        assertEquals((x*7.0+3.0).pow(5.0).pow(1.0/6).grad(x),
//            Const(1.0/6)*(x*7.0+3.0).pow(5.0).pow(-5.0/6.0)*
//                    (Const(5.0)*(x*7.0+3.0).pow(4.0)*(one*(Const(7.0)+x*Const())+Const())
//                            +((x*7.0+3.0).pow(5.0)*SimpleLn(x*7.0+3.0)*Const())
//                            ) +
//                    (x*7.0+3.0).pow(5.0).pow(1.0/6)*SimpleLn((x*7.0+3.0).pow(5.0))*Const())
//        assertEquals(SimpleTan(x).grad(x),(SimpleSin(x)/SimpleCos(x)).grad(x)*one) // Обрати внимание: (Tan(x))' = Tan'(x)*x'=Tan'(x)*1
//                //TEST for different Var
//        assertEquals(SimpleSin(x).grad(y),SimpleCos(x)*Const())
//        assertEquals(SimpleCos(x).grad(y),-SimpleSin(x)*Const())
//        assertEquals(SimpleASin(x).grad(y),SimpleSqrt(-x.pow(2.0)+1.0).inverse()*Const())
//        assertEquals(SimpleACos(x).grad(y),-SimpleSqrt(-x.pow(2.0)+1.0).inverse()*Const())
//        assertEquals(SimpleATan(x).grad(y),(x.pow(2.0)+1.0).inverse()*Const())
//        assertEquals(SimpleExp(x).grad(y),SimpleExp(x)*Const())
//        assertEquals(SimpleLn(x).grad(y),x.inverse()*Const())
//        assertEquals((x+x).grad(y),Const())
//        assertEquals((x*x).grad(y),x*Const()+x*Const())
//        assertEquals(x.pow(x).grad(y),x*x.pow(x-1.0)*Const()+x.pow(x)*SimpleLn(x)*Const())
//        assertEquals(SimpleSin(x).pow(2.0).grad(y),Const(2.0)*SimpleSin(x).pow(one)*SimpleSin(x).grad(y)+
//                SimpleSin(x).pow(2.0)*SimpleLn(SimpleSin(x))*Const())
//        assertEquals((x*7.0+3.0).pow(5.0).pow(1.0/6).grad(y),
//            Const(1.0/6)*(x*7.0+3.0).pow(5.0).pow(-5.0/6.0)*
//                    (Const(5.0)*(x*7.0+3.0).pow(4.0)*(one*(Const()+x*Const())+Const())
//                            +((x*7.0+3.0).pow(5.0)*SimpleLn(x*7.0+3.0)*Const())
//                            ) +
//                    (x*7.0+3.0).pow(5.0).pow(1.0/6)*SimpleLn((x*7.0+3.0).pow(5.0))*Const())
//            //TEST gradient for set of vars
//        assertEquals(f(f,f).grad(setOf(x))[x],f(f,f).grad(x))
//            //TEST for whole gradient
//        assertEquals(f(f,f).grad(), hashMapOf(x to f(f,f).grad(x),y to f(f,f).grad(y)))
//    }
//    @Test
//    fun testVariable(){
//        //TEST equals and clone
//        assert(Variable(x) == Variable(x).clone())
//        assert(Variable(x)==Variable(x))
//        assert(Variable(y) != Variable(x))
////        assert(one == one)
////        assert(one != negone)
//        //TEST constructors
//        assertEquals(Variable(x),Variable("x"))
//        //TEST sub
//        assertEquals(Variable(x).sub(hashMapOf(x to f)),f)
//        assertEquals(Variable(x).sub(hashMapOf(y to f)),Variable(x))
//        //TEST evaluate
//        assertEquals(Variable(x).evaluate(),Variable(x))
//        //TEST getVarsAsArgs
//        assertEquals(Variable(x).getVarsAsArgs(),Args(x))
//        //TEST grad
//            //TEST for derivative by one argument
//                //TEST for the same Var
//        assertEquals(Variable(x).grad(x),one)
//                //TEST for different Var
//        assertEquals(Variable(x).grad(y),Const())
//            //TEST gradient for set of vars
//        assertEquals(Variable(x).grad(setOf(x))[x],one)
//        assertEquals(Variable(x).grad(setOf(y))[y],Const())
//        //TEST for whole gradient
//        assertEquals(Variable(x).grad(), hashMapOf<Var,Func>(x to one))
//    }
//    @Test
//    fun testConst(){
//        //TEST equals and clone
//        assert(one == one.clone())
//        assert(one == one)
//        assert(one != negone)
//        //TEST sub
//        assertEquals(one.sub(hashMapOf(x to f)),one)
//        //TEST evaluate
//        assertEquals(one.evaluate(),one)
//        //TEST getVarsAsArgs
//        assertEquals(one.getVarsAsArgs(),Args())
//        //TEST grad
//            //TEST for derivative by one argument
//                //TEST for the same Var
//        assertEquals(one.grad(x),Const())
//            //TEST gradient for set of vars
//        assertEquals(one.grad(setOf(x))[x],Const())
//        //TEST for whole gradient
//        assertEquals(one.grad(), hashMapOf())
//    }
//}
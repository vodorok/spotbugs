package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XMethod;

/**
 * This detector can find constructors that throw exception.
 */
public class AssertArgs extends AssertDetector {

    //private boolean wasArg = false;

    public AssertArgs(BugReporter bugReporter) {
        super(bugReporter);
    }

    /**
     * Visit a class to find the constructor, then collect all the methods that gets called in it.
     * Also we are checking for final declaration on the class, or a final finalizer, as if present
     * no finalizer attack can happen.
     */
    @Override
    public void visit(Method obj) {
        if (!obj.isPublic()) return;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass ctx = classContext.getJavaClass();
        // Break out of analyzing this class if not public
        if (!ctx.isPublic()) return;
        super.visitClassContext(classContext);
    }

    private boolean isInitialArg() {
        XMethod m = getXMethodOperand();
        int numPar = m.getNumParams();
        // Get values from the stack
        for (int i = 0; i < numPar ; i++) {
            Item item = stack.getStackItem(i);
            if (item.isInitialParameter()) return true;
        }
        return false;
    }

    private boolean isMethodCall(int seen) {
        boolean methodCall = false;
        if (seen == Const.INVOKESTATIC ||
            seen == Const.INVOKEVIRTUAL ||
            seen == Const.INVOKEINTERFACE ||
            seen == Const.INVOKESPECIAL) {
            methodCall = true;
        }
        return methodCall;
    }

    private int checkSeen(int seen) {
        int stackSize = 0;
        switch (seen) {
            case Const.IFNONNULL:
            case Const.IFNULL:
                stackSize = 1;
                break;
            case Const.IF_ICMPEQ:
            case Const.IF_ICMPNE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPLE:
            case Const.IF_ICMPGT:
            case Const.IF_ICMPGE:
                stackSize = 2;
                break;
            default:
                break;
        }
        return stackSize;
    }

    @Override
    protected void resetState() {
        //wasArg = false;
        super.resetState();
    }

	@Override
	void detect(int seen) {
        boolean wasArg = false;
        if (isMethodCall(seen)) {
            // If wasArg have not been found - Nested methods
            if (!wasArg) wasArg = isInitialArg();
        }
        int stackSize = checkSeen(seen);
        if (stackSize > 0) {
            for (int i = 0; i < stackSize; i++) {
                OpcodeStack.Item item = stack.getStackItem(i);
                if (!wasArg) wasArg = item.isInitialParameter();
            }
        }
        if (wasArg) {
            BugInstance bug = new BugInstance(this, "DA_DONT_ASSERT_ARGS", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addSourceLine(this, getPC());
            reportBug(bug);
        }
        wasArg = false;
            //resetState();
	}
}

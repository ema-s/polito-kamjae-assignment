package com.kamjae.coiote;

/**
 * Custom GA exception class
 * @author Jeff Smith jeff@SoftTechDesign.com
 */
public class GAException extends Exception
{
    /**
     * GAException constructor
     * @param msg
     */
    GAException(String msg)
    {
        super(msg);
    }
}
